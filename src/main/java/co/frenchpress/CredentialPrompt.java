package co.frenchpress;

/**
 * SPI for collecting Steam credentials interactively.
 *
 * <p>frenchpress ships {@link SwingCredentialPrompt} as the built-in fallback
 * for desktop environments. A host application (e.g. KnightLauncher-Android)
 * can supply a native implementation without leaking platform types into this
 * module, by either:
 * <ul>
 *   <li>calling {@link #register} before {@link com.threerings.froth.SteamAPI#init()}
 *       runs, or</li>
 *   <li>setting system property {@code frenchpress.credentialPrompt} to an
 *       implementing class name with a public no-arg constructor, or</li>
 *   <li>registering one via {@link java.util.ServiceLoader}.</li>
 * </ul>
 */
public interface CredentialPrompt {

  /**
   * Prompt the user for their Steam username and password.
   *
   * @return credentials to use for Steam login, or {@code null}/credentials
   *         where {@link Credentials#isWebAccount()} is true if the user
   *         wants to use a Three Rings / Grey Havens web account instead
   */
  Credentials promptForLogin ();

  /**
   * Prompt for a Steam Guard authenticator (TOTP) code.
   *
   * <p>The user may also approve the sign-in from the Steam Mobile App
   * without entering a code. Returning {@code null} or an empty string
   * signals that preference and lets JavaSteam poll for app confirmation.
   *
   * @param prevWrong true if the previous code was rejected
   * @return the authenticator code, or {@code null}/empty to wait for
   *         Steam Mobile App approval instead
   */
  String promptForDeviceCode (boolean prevWrong);

  /**
   * Prompt for a Steam Guard email code.
   *
   * <p>The user may also approve the sign-in from the Steam Mobile App
   * without entering a code. Returning {@code null} or an empty string
   * signals that preference and lets JavaSteam poll for app confirmation.
   *
   * @param email     the address the code was sent to
   * @param prevWrong true if the previous code was rejected
   * @return the email code, or {@code null}/empty to wait for
   *         Steam Mobile App approval instead
   */
  String promptForEmailCode (String email, boolean prevWrong);

  /**
   * Registers an implementation, overriding auto-detection.  Call early —
   * before {@link com.threerings.froth.SteamAPI#init()} — so the prompt is
   * in place before any login attempt is made.
   *
   * <p>This is the recommended integration point for Android hosts, which
   * cannot use ServiceLoader or system properties easily.
   */
  static void register (CredentialPrompt p) {
    Holder.INSTANCE = p;
  }

  /**
   * Resolves the active prompt in priority order:
   * <ol>
   *   <li>Instance registered via {@link #register}</li>
   *   <li>Class named by system property {@code frenchpress.credentialPrompt}</li>
   *   <li>{@link java.util.ServiceLoader} discovery</li>
   *   <li>{@link SwingCredentialPrompt} if AWT/Swing is available</li>
   *   <li>{@code null} (no interactive prompt; fall back to env vars or fail)</li>
   * </ol>
   */
  static CredentialPrompt resolve () {
    if (Holder.INSTANCE != null) return Holder.INSTANCE;

    String cls = System.getProperty("frenchpress.credentialPrompt");
    if (cls != null && !cls.isEmpty()) {
      try {
        return (CredentialPrompt) Class.forName(cls)
          .getDeclaredConstructor().newInstance();
      } catch (Throwable t) {
        System.err.println("[frenchpress] credentialPrompt '" + cls
          + "' failed to load (" + t + "); falling back");
      }
    }

    try {
      var it = java.util.ServiceLoader.load(CredentialPrompt.class).iterator();
      if (it.hasNext()) return it.next();
    } catch (Throwable t) {
      System.err.println("[frenchpress] ServiceLoader for CredentialPrompt failed: " + t);
    }

    return SwingCredentialPrompt.createIfSupported();
  }

  /** Holds the statically-registered instance (avoids a public mutable field). */
  final class Holder {
    static volatile CredentialPrompt INSTANCE;
    private Holder () {}
  }
}
