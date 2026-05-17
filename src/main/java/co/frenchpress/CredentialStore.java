package co.frenchpress;

/**
 * Pluggable persistence for the Steam refresh token (and Steam Guard machine
 * token) so relaunches skip password + 2FA.
 *
 * <p>The blob handed to {@link #save} / returned by {@link #load} is opaque to
 * callers; treat it as a single secret string. It is roughly as sensitive as a
 * password at rest, so implementations are expected to protect it.
 *
 * <p>frenchpress ships {@link FileCredentialStore} (a private-dir,
 * perms-locked file) as the default. A host application can inject a
 * stronger, OS-backed implementation without leaking platform types
 * into this JDK module, by either:
 * <ul>
 *   <li>setting system property {@code frenchpress.credentialStore} to an
 *       implementing class name with a public no-arg constructor, or</li>
 *   <li>registering one via {@link java.util.ServiceLoader}.</li>
 * </ul>
 */
public interface CredentialStore {

  /** @return the previously saved blob, or {@code null} if none/unreadable. */
  String load ();

  /** Persist {@code data}, replacing any prior value. */
  void save (String data);

  /** Remove any stored blob (e.g. after the token is rejected as expired). */
  void clear ();

  /**
   * Resolves the active store: explicit system property first, then
   * {@link java.util.ServiceLoader}, then the default file-backed store.
   */
  static CredentialStore resolve () {
    String cls = System.getProperty("frenchpress.credentialStore");
    if (cls != null && !cls.isEmpty()) {
      try {
        return (CredentialStore) Class.forName(cls)
          .getDeclaredConstructor().newInstance();
      } catch (Throwable t) {
        System.err.println("[frenchpress] credentialStore '" + cls
          + "' failed to load (" + t + "); falling back to default");
      }
    }
    try {
      var it = java.util.ServiceLoader.load(CredentialStore.class).iterator();
      if (it.hasNext()) return it.next();
    } catch (Throwable t) {
      System.err.println("[frenchpress] ServiceLoader for CredentialStore failed: " + t);
    }
    return new FileCredentialStore();
  }
}
