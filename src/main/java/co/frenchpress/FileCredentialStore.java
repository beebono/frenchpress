package co.frenchpress;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

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
      return s.isEmpty() ? null : s;
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
      Files.writeString(p, data);
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
