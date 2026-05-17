package com.threerings.froth;

/**
 * Shim — SK references these enums and data classes as types but never
 * invokes any method here, so the bodies all throw rather than pretend to
 * succeed.
 */
public class SteamInput
{
  public static final int MAX_COUNT = 16;
  public static final int MAX_ACTIVE_LAYERS = 16;
  public static final int MAX_ORIGINS = 8;
  public static final long HANDLE_ALL_CONTROLLERS = -1L;

  public enum InputSourceMode {
    NONE, DPAD, BUTTONS, FOUR_BUTTONS, ABSOLUTE_MOUSE, RELATIVE_MOUSE,
    JOYSTICK_MOVE, JOYSTICK_MOUSE, JOYSTICK_CAMERA, SCROLL_WHEEL,
    TRIGGER, TOUCH_MENU, MOUSE_JOYSTICK, MOUSE_REGION, RADIAL_MENU,
    SINGLE_BUTTON, SWITCHES,
    ;
  }

  public enum InputType {
    UNKNOWN,
    STEAM_CONTROLLER,
    XBOX_360_CONTROLLER,
    XBOX_ONE_CONTROLLER,
    GENERIC_GAMEPAD,
    PS4_CONTROLLER,
    APPLE_MFI_CONTROLLER,
    ANDROID_CONTROLLER,
    SWITCH_JOYCON_PAIR,
    SWITCH_JOYCON_SINGLE,
    SWITCH_PRO_CONTROLLER,
    MOBILE_TOUCH,
    PS3_CONTROLLER,
    PS5_CONTROLLER,
    STEAM_DECK_CONTROLLER,
    ;
  }

  public enum GlyphSize { SMALL, MEDIUM, LARGE, ; }

  public enum LEDFlag { SET_COLOR, RESTORE_USER_DEFAULT, ; }

  @Deprecated
  public enum ControllerPad { LEFT, RIGHT, ; }

  public static final class DigitalActionData {
    public boolean state;
    public boolean active;
    @Override public String toString () {
      return "DigitalActionData{state=" + state + ", active=" + active + "}";
    }
  }

  public static final class AnalogActionData {
    public InputSourceMode mode;
    public float x;
    public float y;
    public boolean active;
    @Override public String toString () {
      return "AnalogActionData{mode=" + mode + ", x=" + x + ", y=" + y
        + ", active=" + active + "}";
    }
  }

  public static final class MotionData {
    public float rotQuatX, rotQuatY, rotQuatZ, rotQuatW;
    public float posAccelX, posAccelY, posAccelZ;
    public float rotVelX, rotVelY, rotVelZ;
  }

  private static UnsupportedOperationException unused () {
    return new UnsupportedOperationException("frenchpress: SteamInput unused");
  }

  public static boolean init (boolean explicitlyCallRunFrame) { throw unused(); }
  public static boolean shutdown () { throw unused(); }
  public static boolean setInputActionManifestFilePath (String path) { throw unused(); }
  public static void runFrame () { throw unused(); }
  public static boolean waitForData (boolean waitForever, int timeout) { throw unused(); }
  public static boolean newDataAvailable () { throw unused(); }
  public static int getConnectedControllers (long[] handlesOut) { throw unused(); }
  public static void enableDeviceCallbacks () { throw unused(); }
  public static long getActionSetHandle (String actionSetName) { throw unused(); }
  public static void activateActionSet (long inputHandle, long actionSetHandle) { throw unused(); }
  public static long getCurrentActionSet (long inputHandle) { throw unused(); }
  public static void activateActionSetLayer (long inputHandle, long actionSetLayerHandle) { throw unused(); }
  public static void deactivateActionSetLayer (long inputHandle, long actionSetLayerHandle) { throw unused(); }
  public static void deactivateAllActionSetLayers (long inputHandle) { throw unused(); }
  public static int getActiveActionSetLayers (long inputHandle, long[] handlesOut) { throw unused(); }
  public static long getDigitalActionHandle (String actionName) { throw unused(); }
  public static boolean getDigitalActionData (
      long inputHandle, long digitalActionHandle, DigitalActionData data) { throw unused(); }
  public static int getDigitalActionOrigins (
      long inputHandle, long actionSetHandle, long digitalActionHandle, int[] originsOut) {
    throw unused();
  }
  public static String getStringForDigitalActionName (long digitalActionHandle) { throw unused(); }
  public static long getAnalogActionHandle (String actionName) { throw unused(); }
  public static boolean getAnalogActionData (
      long inputHandle, long analogActionHandle, AnalogActionData data) { throw unused(); }
  public static int getAnalogActionOrigins (
      long inputHandle, long actionSetHandle, long analogActionHandle, int[] originsOut) {
    throw unused();
  }
  public static String getStringForAnalogActionName (long analogActionHandle) { throw unused(); }
  public static void stopAnalogActionMomentum (long inputHandle, long analogActionHandle) { throw unused(); }
  public static String getGlyphPNGForActionOrigin (int origin, GlyphSize size, int flags) { throw unused(); }
  public static String getGlyphSVGForActionOrigin (int origin, int flags) { throw unused(); }
  @Deprecated
  public static String getGlyphForActionOriginLegacy (int origin) { throw unused(); }
  public static String getStringForActionOrigin (int origin) { throw unused(); }
  public static String getStringForXboxOrigin (int origin) { throw unused(); }
  public static String getGlyphForXboxOrigin (int origin) { throw unused(); }
  public static int getActionOriginFromXboxOrigin (long inputHandle, int xboxOrigin) { throw unused(); }
  public static int translateActionOrigin (InputType destinationType, int sourceOrigin) { throw unused(); }
  public static boolean getMotionData (long inputHandle, MotionData data) { throw unused(); }
  public static void triggerVibration (long inputHandle, int leftSpeed, int rightSpeed) { throw unused(); }
  public static void triggerVibrationExtended (
      long inputHandle, int leftSpeed, int rightSpeed,
      int leftTriggerSpeed, int rightTriggerSpeed) {
    throw unused();
  }
  public static void triggerSimpleHapticEvent (
      long inputHandle, int hapticLocation,
      int intensity, int gainDB, int otherIntensity, int otherGainDB) {
    throw unused();
  }
  public static void setLEDColor (long inputHandle, int colorR, int colorG, int colorB, LEDFlag flag) {
    throw unused();
  }
  @Deprecated
  public static void legacyTriggerHapticPulse (
      long inputHandle, ControllerPad targetPad, int durationMicroSec) {
    throw unused();
  }
  @Deprecated
  public static void legacyTriggerRepeatedHapticPulse (
      long inputHandle, ControllerPad targetPad,
      int durationMicroSec, int offMicroSec, int repeat, int flags) {
    throw unused();
  }
  public static boolean showBindingPanel (long inputHandle) { throw unused(); }
  public static InputType getInputTypeForHandle (long inputHandle) { throw unused(); }
  public static long getControllerForGamepadIndex (int index) { throw unused(); }
  public static int getGamepadIndexForController (long inputHandle) { throw unused(); }
  public static boolean getDeviceBindingRevision (long inputHandle, int[] revisionOut) { throw unused(); }
  public static int getRemotePlaySessionID (long inputHandle) { throw unused(); }
  public static int getSessionInputConfigurationSettings () { throw unused(); }
}
