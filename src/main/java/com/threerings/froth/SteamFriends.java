package com.threerings.froth;

import java.util.concurrent.CopyOnWriteArrayList;

import co.frenchpress.SteamSession;

/**
 * Shim re-implementation of froth-foamy's {@code SteamFriends}. The public
 * surface (including inner enums and callback interfaces) matches verbatim
 * so SK code that references types here resolves identically. Most methods
 * are safe-default no-ops; only {@link #getPersonaName()} returns a dummy
 * value, since SK seems to check it, but uses the character name instead.
 */
public class SteamFriends
{
  public enum PersonaState {
    OFFLINE, ONLINE, BUSY, AWAY, SNOOZE, LOOKING_TO_TRADE, LOOKING_TO_PLAY,
    UNKNOWN
  }

  public enum OverlayToStoreFlag {
    NONE, ADD_TO_CART, ADD_TO_CART_AND_SHOW
  }

  public enum WebPageMode {
    DEFAULT, MODAL,
    ;
  }

  public interface GameOverlayActivationCallback
  {
    void gameOverlayActivated (boolean active);
  }

  public interface GameRichPresenceJoinRequestCallback
  {
    void gameRichPresenceJoinRequested (long steamIdFriend, String connect);
  }

  public static final int FRIEND_FLAG_IMMEDIATE = 0x04;
  public static final String STATUS_KEY = "status";
  public static final String CONNECT_KEY = "connect";

  public static void addGameOverlayActivationCallback (GameOverlayActivationCallback callback) {
    _overlayCbs.add(callback);
  }
  public static void removeGameOverlayActivationCallback (GameOverlayActivationCallback callback) {
    _overlayCbs.remove(callback);
  }

  public static void addGameRichPresenceJoinRequestCallback (
      GameRichPresenceJoinRequestCallback callback) {
    _joinReqCbs.add(callback);
  }
  public static void removeGameRichPresenceJoinRequestCallback (
      GameRichPresenceJoinRequestCallback callback) {
    _joinReqCbs.remove(callback);
  }

  public static String getPersonaName () {
    SteamSession s = SteamSession.get();
    return s != null ? s.personaName() : "Player";
  }

  public static int getFriendCount (int flags) { return 0; }

  public static long getFriendByIndex (int index, int flags) { return 0L; }

  public static PersonaState getFriendPersonaState (long steamId) {
    return PersonaState.OFFLINE;
  }

  public static String getFriendPersonaName (long steamId) { return ""; }

  public static void setInGameVoiceSpeaking (long steamId, boolean speaking) {}

  public static void activateGameOverlayToWebPage (String url) {}
  public static void activateGameOverlayToWebPage (String url, WebPageMode mode) {
    if (mode == null) throw new NullPointerException("mode");
  }

  public static void activateGameOverlayToStore (int appId, OverlayToStoreFlag flag) {}

  public static boolean setRichPresence (String key, String value) { return true; }

  public static String getFriendRichPresence (long steamId, String key) { return ""; }

  public static boolean inviteUserToGame (long steamIdFriend, String connect) { return false; }

  private static final CopyOnWriteArrayList<GameOverlayActivationCallback>
    _overlayCbs = new CopyOnWriteArrayList<>();
  private static final CopyOnWriteArrayList<GameRichPresenceJoinRequestCallback>
    _joinReqCbs = new CopyOnWriteArrayList<>();
}
