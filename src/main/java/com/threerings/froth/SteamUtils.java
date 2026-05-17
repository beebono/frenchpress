package com.threerings.froth;

import java.util.concurrent.CopyOnWriteArrayList;

import co.frenchpress.SteamSession;

public class SteamUtils
{
  public enum NotificationPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

  public enum FloatingGamepadTextInputMode {
    SINGLE_LINE, MULTIPLE_LINES, EMAIL, NUMERIC,
    ;
  }

  public enum GamepadTextInputMode {
    NORMAL, PASSWORD,
    ;
  }

  public enum GamepadTextInputLineMode {
    SINGLE_LINE, MULTIPLE_LINES,
    ;
  }

  public interface WarningMessageHook
  {
    void warning (int severity, String message);
  }

  public interface FloatingGamepadTextInputDismissedCallback
  {
    void floatingGamepadTextInputDismissed ();
  }

  public interface AppResumingFromSuspendCallback
  {
    void appResumingFromSuspend ();
  }

  public interface GamepadTextInputDismissedCallback
  {
    void gamepadTextInputDismissed (boolean submitted, int submittedText, int appId);
  }

  public static int getAppID () { return SteamSession.SK_APPID; }

  public static void setOverlayNotificationPosition (NotificationPosition position) {}

  public static void setWarningMessageHook (WarningMessageHook hook) {}

  public static boolean isOverlayEnabled () { return false; }
  public static boolean overlayNeedsPresent () { return false; }

  public static boolean showFloatingGamepadTextInput (
      FloatingGamepadTextInputMode keyboardMode,
      int textFieldXPosition, int textFieldYPosition,
      int textFieldWidth, int textFieldHeight) {
    return false;
  }
  public static boolean dismissFloatingGamepadTextInput () { return false; }

  public static boolean showGamepadTextInput (
      GamepadTextInputMode mode, GamepadTextInputLineMode lineMode,
      String description, int charMax, String existingText) {
    if (mode == null) throw new NullPointerException("mode");
    if (lineMode == null) throw new NullPointerException("lineMode");
    return false;
  }

  public static int getEnteredGamepadTextLength () { return 0; }
  public static String getEnteredGamepadTextInput () { return null; }
  public static boolean dismissGamepadTextInput () { return false; }

  public static void addFloatingGamepadTextInputDismissedCallback (
      FloatingGamepadTextInputDismissedCallback callback) {
    _floatingCbs.add(callback);
  }
  public static void removeFloatingGamepadTextInputDismissedCallback (
      FloatingGamepadTextInputDismissedCallback callback) {
    _floatingCbs.remove(callback);
  }

  public static void addAppResumingFromSuspendCallback (AppResumingFromSuspendCallback callback) {
    _resumeCbs.add(callback);
  }
  public static void removeAppResumingFromSuspendCallback (
      AppResumingFromSuspendCallback callback) {
    _resumeCbs.remove(callback);
  }

  public static void addGamepadTextInputDismissedCallback (
      GamepadTextInputDismissedCallback callback) {
    _gamepadCbs.add(callback);
  }
  public static void removeGamepadTextInputDismissedCallback (
      GamepadTextInputDismissedCallback callback) {
    _gamepadCbs.remove(callback);
  }

  public static boolean isSteamRunningOnSteamDeck () { return false; }

  private static final CopyOnWriteArrayList<FloatingGamepadTextInputDismissedCallback>
    _floatingCbs = new CopyOnWriteArrayList<>();
  private static final CopyOnWriteArrayList<GamepadTextInputDismissedCallback>
    _gamepadCbs = new CopyOnWriteArrayList<>();
  private static final CopyOnWriteArrayList<AppResumingFromSuspendCallback>
    _resumeCbs = new CopyOnWriteArrayList<>();
}
