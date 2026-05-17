package com.threerings.froth;

/**
 * Shim — SK references these enums/callback interfaces as types but never
 * invokes any method here, so the bodies all throw rather than pretend to
 * succeed.
 */
public class SteamMatchmaking
{
  public enum LobbyType {
    PRIVATE, FRIENDS_ONLY, PUBLIC, INVISIBLE, PRIVATE_UNIQUE
  }

  public enum Result { OK, NO_CONNECTION, TIMEOUT, FAIL, ACCESS_DENIED, LIMIT_EXCEEDED }

  public enum ChatRoomEnterResponse {
    SUCCESS, DOESNT_EXIST, NOT_ALLOWED, FULL, ERROR,
    BANNED, LIMITED, CLAN_DISABLED, COMMUNITY_BAN,
    MEMBER_BLOCKED_YOU, YOU_BLOCKED_MEMBER,
    RATELIMIT_EXCEEDED
  }

  public interface CreateLobbyCallback
  {
    void createLobbyResponse (Result result, long steamIdLobby);
  }

  public interface EnterLobbyCallback
  {
    void enterLobbyResponse (
      long steamIdLobby, int chatPermissions, boolean locked,
      ChatRoomEnterResponse response);
  }

  public interface GameLobbyJoinRequestCallback
  {
    void gameLobbyJoinRequest (long steamIdLobby, long steamIdFriend);
  }

  public static void addGameLobbyJoinRequestCallback (GameLobbyJoinRequestCallback callback) {
    throw new UnsupportedOperationException("frenchpress: SteamMatchmaking unused");
  }
  public static void removeGameLobbyJoinRequestCallback (GameLobbyJoinRequestCallback callback) {
    throw new UnsupportedOperationException("frenchpress: SteamMatchmaking unused");
  }

  public static void createLobby (LobbyType type, int maxMembers, CreateLobbyCallback callback) {
    throw new UnsupportedOperationException("frenchpress: SteamMatchmaking unused");
  }
  public static void joinLobby (long steamIdLobby, EnterLobbyCallback callback) {
    throw new UnsupportedOperationException("frenchpress: SteamMatchmaking unused");
  }
  public static void leaveLobby (long steamIdLobby) {
    throw new UnsupportedOperationException("frenchpress: SteamMatchmaking unused");
  }
  public static boolean inviteUserToLobby (long steamIdLobby, long steamIdInvitee) {
    throw new UnsupportedOperationException("frenchpress: SteamMatchmaking unused");
  }
  public static String getLobbyData (long steamIdLobby, String key) {
    throw new UnsupportedOperationException("frenchpress: SteamMatchmaking unused");
  }
  public static boolean setLobbyData (long steamIdLobby, String key, String value) {
    throw new UnsupportedOperationException("frenchpress: SteamMatchmaking unused");
  }
}
