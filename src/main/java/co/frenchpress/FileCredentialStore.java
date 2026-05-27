package co.frenchpress;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Default {@link CredentialStore}: a single file in a private directory,
 * locked to owner-only permissions where the filesystem supports POSIX modes.
 *
 * <p>Location, in order of precedence:
 * <ol>
 *   <li>{@code FRENCHPRESS_CRED_FILE} env var (absolute path), else</li>
 *   <li>{@code $XDG_DATA_HOME/frenchpress/steam.creds}, else</li>
 *   <li>{@code ~/.local/share/frenchpress/steam.creds}</li>
 * </ol>
 */
public final class FileCredentialStore implements CredentialStore {

  @Override public String load () {
    try {
      Path p = path();
      if (!Files.isReadable(p)) return null;
      String s = Files.readString(p).strip();
      if (s.isEmpty()) return null;
      try {
        return decrypt(s);
      } catch (Exception e) {
        // Fallback for legacy plaintext credentials
        return s;
      }
    } catch (Throwable t) {
      System.err.println("[frenchpress] credential load failed: " + t);
      return null;
    }
  }

  @Override public void save (String data) {
    try {
      Path p = path();
      Path dir = p.getParent();
      if (dir != null) {
        Files.createDirectories(dir);
        trySetPerms(dir, Set.of(PosixFilePermission.OWNER_READ,
          PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
      }
      Files.writeString(p, encrypt(data));
      trySetPerms(p, Set.of(PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE));
    } catch (Throwable t) {
      System.err.println("[frenchpress] credential save failed: " + t);
    }
  }

  @Override public void clear () {
    try { Files.deleteIfExists(path()); }
    catch (Throwable t) { System.err.println("[frenchpress] credential clear failed: " + t); }
  }

  private static void trySetPerms (Path p, Set<PosixFilePermission> perms) {
    try { Files.setPosixFilePermissions(p, perms); }
    catch (UnsupportedOperationException | IOException ignored) {
      // Non-POSIX FS, rely on dir isolation.
    }
  }

  private static SecretKey getOrGenerateKey () throws Exception {
    Preferences prefs = Preferences.userNodeForPackage(FileCredentialStore.class);
    String encodedKey = prefs.get("frenchpress_aes_key", null);
    if (encodedKey != null) {
      byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
      return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(128);
    SecretKey secretKey = keyGen.generateKey();
    prefs.put("frenchpress_aes_key", Base64.getEncoder().encodeToString(secretKey.getEncoded()));
    return secretKey;
  }

  private static String encrypt (String data) throws Exception {
    SecretKey key = getOrGenerateKey();
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    byte[] iv = new byte[12];
    new SecureRandom().nextBytes(iv);
    GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
    byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

    ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
    byteBuffer.put(iv);
    byteBuffer.put(encryptedData);
    return Base64.getEncoder().encodeToString(byteBuffer.array());
  }

  private static String decrypt (String encryptedData) throws Exception {
    SecretKey key = getOrGenerateKey();
    byte[] cipherMessage = Base64.getDecoder().decode(encryptedData);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    GCMParameterSpec parameterSpec = new GCMParameterSpec(128, cipherMessage, 0, 12);
    cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
    byte[] plainText = cipher.doFinal(cipherMessage, 12, cipherMessage.length - 12);
    return new String(plainText, StandardCharsets.UTF_8);
  }

  private static Path path () {
    String override = System.getenv("FRENCHPRESS_CRED_FILE");
    if (override != null && !override.isEmpty()) return Path.of(override);

    String xdg = System.getenv("XDG_DATA_HOME");
    Path base = (xdg != null && !xdg.isEmpty())
      ? Path.of(xdg)
      : Path.of(System.getProperty("user.home", "."), ".local", "share");
    return base.resolve("frenchpress").resolve("steam.creds");
  }
}
