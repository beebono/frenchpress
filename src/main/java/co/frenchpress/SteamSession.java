package co.frenchpress;

import in.dragonbra.javasteam.enums.EResult;
import in.dragonbra.javasteam.steam.authentication.AuthPollResult;
import in.dragonbra.javasteam.steam.authentication.AuthSessionDetails;
import in.dragonbra.javasteam.steam.authentication.IAuthenticator;
import in.dragonbra.javasteam.steam.handlers.steamauthticket.SteamAuthTicket;
import in.dragonbra.javasteam.steam.handlers.steamauthticket.TicketInfo;
import in.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails;
import in.dragonbra.javasteam.steam.handlers.steamuser.SteamUser;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback;
import in.dragonbra.javasteam.steam.steamclient.SteamClient;
import in.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager;
import in.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback;
import in.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaSteam-backed Steam session, owned by the shim and lazily logged in
 * the first time {@link com.threerings.froth.SteamAPI#init()} runs.
 *
 * Credential acquisition priority:
 *  1. Stored refresh token (CredentialStore) — silent, no UI shown.
 *  2. FRENCHPRESS_STEAM_USER / FRENCHPRESS_STEAM_PASS env vars — for
 *     headless / scripted use; still shows the 2FA prompt if needed.
 *  3. Interactive UI via {@link CredentialPrompt} — shown on first run
 *     and whenever the stored token expires or is revoked.
 *     Submitting empty credentials skips Steam login (web account path).
 *
 * Android hosts should call {@link CredentialPrompt#register} early with
 * a native dialog implementation.  Desktop environments get
 * {@link SwingCredentialPrompt} automatically.
 */
public final class SteamSession {

  /** Spiral Knights' Steam AppID. */
  public static final int SK_APPID = 99900;

  /**
   * How long to wait for the user to approve a sign-in before giving up.
   * Steam drops the CM WebSocket on its own at ~64–69s of an unanswered
   * device-confirmation poll (observed; likely a server heartbeat/idle timeout),
   * which surfaces as a CancellationException out of pollingWaitForResult. We cut
   * just under that so the timeout path runs first and tears down via our own
   * user-initiated disconnect — a clean abort instead of that server-side drop.
   * (Drop toward ~55s if a CancellationException ever still sneaks in.)
   */
  private static final long AUTH_WAIT_SECONDS = 60;

  /** @return the active session, logging in on first call. Returns null if login fails or the user chose a web account. */
  public static synchronized SteamSession get () {
    if (INSTANCE == null && !LOGIN_ATTEMPTED) {
      LOGIN_ATTEMPTED = true;
      INSTANCE = login();
    }
    return INSTANCE;
  }

  private static SteamSession login () {
    // JavaSteam's SteamAuthentication encrypts the password with the BC-spelled
    // "RSA/None/PKCS1Padding" via CryptoHelper.SEC_PROV. Register our no-BC shim
    // provider (SEC_PROV) before any auth call so that transformation resolves.
    FrenchpressJceProvider.ensureRegistered();

    CredentialStore store = CredentialStore.resolve();
    String carryGuardData = null;

    // 1. Steady state: a previously stored refresh token skips password + 2FA.
    StoredCreds saved = StoredCreds.parse(store.load());
    if (saved != null) {
      System.err.println("[frenchpress] trying stored refresh token for account "
        + saved.account);
      SteamSession s = attempt(store, saved, null, null, null, null);
      if (s != null) return s;
      // Token rejected (expired/revoked). Keep the guard token to skip 2FA
      // on re-auth, then fall through to credentials.
      System.err.println("[frenchpress] stored token rejected; clearing and "
        + "falling back to credentials");
      carryGuardData = saved.guardData;
      store.clear();
    }

    // 2. Env-var credentials — headless / scripted use.  Still uses the
    //    prompt for 2FA if it fires, so the UI can appear there too.
    String envUser = System.getenv("FRENCHPRESS_STEAM_USER");
    String envPass = System.getenv("FRENCHPRESS_STEAM_PASS");
    if (envUser != null && !envUser.isEmpty()
        && envPass != null && !envPass.isEmpty()) {
      System.err.println("[frenchpress] login() using env-var credentials "
        + "(user len=" + envUser.length() + ")");
      return attempt(store, null, envUser, envPass, carryGuardData,
        CredentialPrompt.resolve());
    }

    // 3. Interactive UI prompt.
    CredentialPrompt prompt = CredentialPrompt.resolve();
    if (prompt == null) {
      System.err.println("[frenchpress] no stored token, no env-var credentials, "
        + "and no UI prompt available; Steam auth will not proceed");
      return null;
    }

    Credentials creds = prompt.promptForLogin();
    if (creds == null || creds.isWebAccount()) {
      System.err.println("[frenchpress] user chose web account; skipping Steam login");
      return null;
    }
    return attempt(store, null, creds.username(), creds.password(),
      carryGuardData, prompt);
  }

  /**
   * One connect+logon lifecycle. Exactly one of {@code saved} (token path) or
   * {@code user}/{@code pass} (credential path) is non-null. On a successful
   * credential login the resulting refresh token + guard data are persisted to
   * {@code store} so subsequent launches take the token path.
   *
   * {@code prompt} is used for 2FA dialogs; may be null (env-var-only or
   * token-resume paths where no UI is expected).
   */
  private static SteamSession attempt (CredentialStore store, StoredCreds saved,
      String user, String pass, String guardData, CredentialPrompt prompt) {
    SteamClient client = new SteamClient();
    CallbackManager manager = new CallbackManager(client);
    SteamUser steamUser = client.getHandler(SteamUser.class);
    SteamAuthTicket authTicket = client.getHandler(SteamAuthTicket.class);
    if (steamUser == null || authTicket == null) {
      System.err.println("[frenchpress] JavaSteam handlers unavailable; aborting login");
      return null;
    }

    SteamSession session = new SteamSession(client, manager, steamUser, authTicket);
    CountDownLatch done = new CountDownLatch(1);
    final boolean[] ok = { false };
    final StoredCreds[] toPersist = { null };

    manager.subscribe(ConnectedCallback.class, cb -> {
      try {
        if (saved != null) {
          System.err.println("[frenchpress] ConnectedCallback; logging on with stored token");
          LogOnDetails logon = new LogOnDetails();
          logon.setUsername(saved.account);
          logon.setAccessToken(saved.refreshToken);
          steamUser.logOn(logon);
          return;
        }

        System.err.println("[frenchpress] ConnectedCallback fired; starting auth flow");
        AuthSessionDetails details = new AuthSessionDetails();
        details.username = user;
        details.password = pass;
        details.persistentSession = true;
        if (guardData != null && !guardData.isEmpty()) details.guardData = guardData;
        details.authenticator = new PushAuthenticator(prompt);

        var authSession = client.getAuthentication().beginAuthSessionViaCredentials(details).get();
        AuthPollResult poll = authSession.pollingWaitForResult().get();

        String newGuard = poll.getNewGuardData();
        toPersist[0] = new StoredCreds(poll.getAccountName(),
          poll.getRefreshToken(),
          (newGuard != null && !newGuard.isEmpty()) ? newGuard : guardData);

        LogOnDetails logon = new LogOnDetails();
        logon.setUsername(poll.getAccountName());
        logon.setAccessToken(poll.getRefreshToken());
        steamUser.logOn(logon);
      } catch (Throwable e) {
        System.err.println("[frenchpress] auth flow failed:");
        e.printStackTrace(System.err);
        Throwable c = e.getCause();
        while (c != null) {
          System.err.println("[frenchpress] caused by:");
          c.printStackTrace(System.err);
          c = c.getCause();
        }
        done.countDown();
      }
    });

    manager.subscribe(LoggedOnCallback.class, cb -> {
      System.err.println("[frenchpress] LoggedOnCallback result=" + cb.getResult());
      if (cb.getResult() != EResult.OK) {
        System.err.println("[frenchpress] logon failed: " + cb.getResult()
          + " / " + cb.getExtendedResult());
        done.countDown();
        return;
      }
      session.steamId = cb.getClientSteamID() != null
        ? cb.getClientSteamID().convertToUInt64() : 0L;
      if (toPersist[0] != null) {
        store.save(toPersist[0].serialize());
        System.err.println("[frenchpress] persisted refresh token for future launches");
      }
      ok[0] = true;
      done.countDown();
    });

    manager.subscribe(DisconnectedCallback.class, cb -> {
      System.err.println("[frenchpress] DisconnectedCallback userInitiated="
        + cb.isUserInitiated());
      if (done.getCount() > 0) done.countDown();
    });

    Thread pump = new Thread(() -> {
      while (session.running) {
        manager.runWaitCallbacks(1000L);
      }
    }, "frenchpress-steam-callbacks");
    pump.setDaemon(true);
    session.pump = pump;
    pump.start();

    System.err.println("[frenchpress] connecting to Steam...");
    // Keep-alive bracket: on Android the host process is killed when the user
    // tabs out to approve the sign-in (Steam Mobile App push, or reading a Steam
    // Guard code), which aborts the poll below and forces a fresh login next
    // launch. setKeepAlive(true) raises the process priority so it survives the
    // tab-out; it must be ON before the flow can prompt the user, and OFF again
    // on EVERY exit (success, failure, the timeout, the interrupt path) so
    // a backgrounded launcher still dies normally the rest of the time.
    try {
      setKeepAlive(true);
      client.connect();
      if (!done.await(AUTH_WAIT_SECONDS, TimeUnit.SECONDS)) {
        System.err.println("[frenchpress] login timed out after " + AUTH_WAIT_SECONDS + "s");
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    } finally {
      setKeepAlive(false);
    }

    if (!ok[0]) {
      System.err.println("[frenchpress] attempt() returning null (auth not OK)");
      session.disconnect();
      return null;
    }
    System.err.println("[frenchpress] attempt() returning session; steamID=" + session.steamId);
    return session;
  }

  /**
   * Toggles the Android sign-in keep-alive via SkBootstrap.nativeSteamKeepAlive,
   * reached by reflection because frenchpress runs in SK's classloader and must
   * target the system-loader SkBootstrap whose natives sklauncher.c registered —
   * the same hop {@link PushAuthenticator#emitLaunchStatus} uses. No-op (logged)
   * off Android or before native registration, so desktop logins are unaffected.
   */
  private static void setKeepAlive (boolean active) {
    try {
      Class<?> sysBoot = Class.forName(
          "com.skarm.launcher.bootstrap.SkBootstrap",
          false,
          ClassLoader.getSystemClassLoader());
      Method m = sysBoot.getDeclaredMethod("nativeSteamKeepAlive", boolean.class);
      m.setAccessible(true);
      m.invoke(null, active);
    } catch (Throwable t) {
      System.err.println("[frenchpress] keep-alive bridge unavailable: " + t);
    }
  }

  // -------------------------------------------------------------------------
  // StoredCreds
  // -------------------------------------------------------------------------

  private static final class StoredCreds {
    final String account;
    final String refreshToken;
    final String guardData; // may be null

    StoredCreds (String account, String refreshToken, String guardData) {
      this.account = account;
      this.refreshToken = refreshToken;
      this.guardData = guardData;
    }

    String serialize () {
      Properties p = new Properties();
      if (account != null) p.setProperty("account", account);
      if (refreshToken != null) p.setProperty("refreshToken", refreshToken);
      if (guardData != null) p.setProperty("guardData", guardData);
      StringWriter w = new StringWriter();
      try { p.store(w, "frenchpress steam credentials"); }
      catch (Exception e) { return ""; }
      return w.toString();
    }

    static StoredCreds parse (String blob) {
      if (blob == null || blob.isBlank()) return null;
      Properties p = new Properties();
      try { p.load(new StringReader(blob)); }
      catch (Exception e) { return null; }
      String acct = p.getProperty("account");
      String tok  = p.getProperty("refreshToken");
      if (acct == null || acct.isEmpty() || tok == null || tok.isEmpty()) return null;
      return new StoredCreds(acct, tok, p.getProperty("guardData"));
    }
  }

  // -------------------------------------------------------------------------
  // PushAuthenticator
  // -------------------------------------------------------------------------

  /**
   * IAuthenticator that prefers Steam Mobile App approval but can accept a
   * typed code if the user supplies one via the credential prompt.
   *
   * For device-code and email-code callbacks the prompt is shown with a
   * message making clear that the user can either approve from the Steam app
   * OR type a code — not that they are required to type one.  Leaving the
   * field empty and clicking OK falls back to app-approval polling.
   */
  private static final class PushAuthenticator implements IAuthenticator {

    private final CredentialPrompt prompt; // null when no UI is available

    PushAuthenticator (CredentialPrompt prompt) {
      this.prompt = prompt;
    }

    @Override public CompletableFuture<String> getDeviceCode (boolean prevWrong) {
      if (prompt != null) {
        String code = prompt.promptForDeviceCode(prevWrong);
        if (code != null && !code.isEmpty())
          return CompletableFuture.completedFuture(code);
      }
      // Empty string → JavaSteam continues polling for app confirmation.
      return CompletableFuture.completedFuture("");
    }

    @Override public CompletableFuture<String> getEmailCode (String email, boolean prevWrong) {
      if (prompt != null) {
        String code = prompt.promptForEmailCode(email, prevWrong);
        if (code != null && !code.isEmpty())
          return CompletableFuture.completedFuture(code);
      }
      return CompletableFuture.completedFuture("");
    }

    @Override public CompletableFuture<Boolean> acceptDeviceConfirmation () {
      emitLaunchStatus("Waiting for Steam Mobile App approval…");
      System.err.println("[frenchpress] waiting for Steam Mobile App approval");
      return CompletableFuture.completedFuture(true);
    }

    private static void emitLaunchStatus (String message) {
      try {
        Class<?> sysBoot = Class.forName(
            "com.skarm.launcher.bootstrap.SkBootstrap",
            false,
            ClassLoader.getSystemClassLoader());
        Method nativeLaunchStatus = sysBoot.getDeclaredMethod("nativeLaunchStatus", String.class);
        nativeLaunchStatus.setAccessible(true);
        nativeLaunchStatus.invoke(null, message);
      } catch (Throwable t) {
        System.err.println("[frenchpress] launch-status bridge unavailable: " + t);
      }
    }
  }

  // -------------------------------------------------------------------------
  // Session methods
  // -------------------------------------------------------------------------

  private SteamSession (SteamClient client, CallbackManager manager,
      SteamUser steamUser, SteamAuthTicket authTicket) {
    this.client = client;
    this.manager = manager;
    this.steamUser = steamUser;
    this.authTicket = authTicket;
  }

  public long steamID () { return steamId; }

  public String personaName () {
    return "Player";
  }

  public int getAuthSessionTicket (ByteBuffer out) {
    try {
      TicketInfo info = authTicket.getAuthSessionTicket(SK_APPID)
        .get(30, TimeUnit.SECONDS);
      byte[] bytes = info.getTicket();
      ByteBuffer dst = out.order(ByteOrder.LITTLE_ENDIAN);
      dst.clear();
      dst.put(bytes);
      dst.flip();
      int handle = NEXT_HANDLE.getAndIncrement();
      synchronized (tickets) { tickets.put(handle, info); }
      return handle;
    } catch (Exception e) {
      System.err.println("[frenchpress] getAuthSessionTicket failed: " + e);
      return 0;
    }
  }

  public void cancelAuthTicket (int handle) {
    TicketInfo info;
    synchronized (tickets) { info = tickets.remove(handle); }
    if (info != null) info.close();
  }

  public void disconnect () {
    running = false;
    try { client.disconnect(); } catch (Exception ignored) {}
  }

  // -------------------------------------------------------------------------
  // Fields
  // -------------------------------------------------------------------------

  private final SteamClient client;
  private final CallbackManager manager;
  private final SteamUser steamUser;
  private final SteamAuthTicket authTicket;
  private final Map<Integer, TicketInfo> tickets = new HashMap<>();
  private volatile long steamId;
  private volatile boolean running = true;
  private Thread pump;

  private static final AtomicInteger NEXT_HANDLE = new AtomicInteger(1);
  private static volatile SteamSession INSTANCE;
  private static volatile boolean LOGIN_ATTEMPTED;
}
