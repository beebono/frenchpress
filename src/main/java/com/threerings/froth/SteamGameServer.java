package com.threerings.froth;

import java.nio.ByteBuffer;

/**
 * Shim — SK references the enums and callback interfaces here as types but
 * the client never invokes any game-server method. Methods throw rather than
 * pretend to succeed.
 */
public class SteamGameServer
{
  public enum ServerMode {
    INVALID, NO_AUTHENTICATION, AUTHENTICATION, AUTHENTICATION_AND_SECURE
  }

  public enum DenyReason {
    INVALID, INVALID_VERSION, GENERIC, NOT_LOGGED_ON, NO_LICENSE, CHEATER,
    LOGGED_IN_ELSEWHERE, UNKNOWN_TEXT, INCOMPATIBLE_ANTICHEAT, MEMORY_CORRUPTION,
    INCOMPATIBLE_SOFTWARE, STEAM_CONNECTION_LOST, STEAM_CONNECTION_ERROR,
    STEAM_RESPONSE_TIMED_OUT, STEAM_VALIDATION_STALLED, STEAM_OWNER_LEFT_GUEST_USER
  }

  public enum BeginAuthSessionResult {
    OK, INVALID_TICKET, DUPLICATE_REQUEST, INVALID_VERSION, GAME_MISMATCH, EXPIRED_TICKET
  }

  public enum AuthSessionResponse {
    OK, USER_NOT_CONNECTED_TO_STEAM, NO_LICENSE_OR_EXPIRED, VAC_BANNED,
    LOGGED_IN_ELSEWHERE, VAC_CHECK_TIMED_OUT, AUTH_TICKET_CANCELED,
    AUTH_TICKET_INVALID_ALREADY_USED, AUTH_TICKET_INVALID,
    PUBLISHER_ISSUED_BAN, AUTH_TICKET_NETWORK_IDENTITY_FAILURE
  }

  @Deprecated
  public interface AuthenticateCallback
  {
    void clientApprove ();
    void clientDeny (DenyReason denyReason, String optionalText);
  }

  public interface AuthSessionCallback
  {
    void validateAuthTicketResponse (AuthSessionResponse authSessionResponse);
  }

  private static UnsupportedOperationException unused () {
    return new UnsupportedOperationException("frenchpress: SteamGameServer unused");
  }

  public static boolean init (
      int ip, short gamePort, short queryPort, ServerMode serverMode, String versionString) {
    throw unused();
  }
  public static boolean isInitialized () { return false; }
  public static void shutdown () {}
  public static void runCallbacks () {}
  public static long getSteamID () { throw unused(); }
  public static BeginAuthSessionResult beginAuthSession (
      ByteBuffer ticket, long steamId, AuthSessionCallback callback) {
    throw unused();
  }
  public static void endAuthSession (long steamId) { throw unused(); }
}
