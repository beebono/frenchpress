package co.frenchpress;

/**
 * Username/password pair returned by {@link CredentialPrompt#promptForLogin}.
 * A null or empty {@code username} means the user chose to use a web account
 * (Three Rings / Grey Havens credentials) instead of a Steam-linked one.
 */
public record Credentials(String username, String password) {

  /** @return true when the user opted out of Steam login. */
  public boolean isWebAccount () {
    return username == null || username.isEmpty();
  }
}
