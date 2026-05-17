package com.threerings.froth;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.CopyOnWriteArrayList;

import co.frenchpress.SteamSession;

/**
 * frenchpress shim replacement for com.threerings.froth.SteamUser.
 *
 * getAuthSessionTicket is the only method here that produces real
 * cryptographic output; everything else either delegates to the
 * SteamSession's lightweight state or returns voice-unavailable defaults.
 */
public class SteamUser
{
  // Inner types preserved verbatim from froth-foamy's public API surface so
  // that classes compiled against froth-foamy (i.e. SK's projectx-pcode.jar)
  // resolve them at runtime.

  public enum VoiceResult {
    OK, NOT_INITIALIZED, NOT_RECORDING, NO_DATA, BUFFER_TOO_SMALL,
    DATA_CORRUPTED, RESTRICTED, UNSUPPORTED_CODEC,
    RECEIVER_OUT_OF_DATE, RECEIVER_DID_NOT_ANSWER
  }

  public interface SteamServerCallback {
    void steamServersConnected ();
    void steamServersDisconnected ();
  }

  public interface MicroTxnCallback {
    void microTxnAuthorizationResponse (int appId, long orderId, boolean authorized);
  }

  public static void addSteamServerCallback (SteamServerCallback cb) {
    _serverCallbacks.add(cb);
  }
  public static void removeSteamServerCallback (SteamServerCallback cb) {
    _serverCallbacks.remove(cb);
  }

  public static void addMicroTxnCallback (MicroTxnCallback cb) {
    _microTxnCallbacks.add(cb);
  }
  public static void removeMicroTxnCallback (MicroTxnCallback cb) {
    _microTxnCallbacks.remove(cb);
  }

  public static boolean isLoggedOn () {
    return SteamSession.get() != null;
  }

  public static long getSteamID () {
    SteamSession s = SteamSession.get();
    return s == null ? 0L : s.steamID();
  }

  public static int getAuthSessionTicket (ByteBuffer ticket) {
    SteamSession s = SteamSession.get();
    if (s == null) {
      throw new IllegalStateException("getAuthSessionTicket: no Steam session");
    }
    return s.getAuthSessionTicket(ticket);
  }

  public static void cancelAuthTicket (int ticketId) {
    SteamSession s = SteamSession.get();
    if (s != null) s.cancelAuthTicket(ticketId);
  }

  // Voice: report unavailable. SK handles this gracefully.

  public static void startVoiceRecording () {}
  public static void stopVoiceRecording () {}

  public static VoiceResult getAvailableVoice (
    IntBuffer compressed, IntBuffer uncompressed, int desiredSampleRate) {
    return VoiceResult.NO_DATA;
  }

  public static VoiceResult getVoice (
    ByteBuffer compressed, ByteBuffer uncompressed, int desiredSampleRate) {
    return VoiceResult.NO_DATA;
  }

  public static VoiceResult decompressVoice (
    ByteBuffer compressed, ByteBuffer dest, int desiredSampleRate) {
    return VoiceResult.NO_DATA;
  }

  public static int getVoiceOptimalSampleRate () { return 16000; }

  /** Package-private hook for SteamAPI.runCallbacks() to fire the post-init connected event. */
  static void fireServersConnected () {
    for (SteamServerCallback cb : _serverCallbacks) cb.steamServersConnected();
  }

  private static final CopyOnWriteArrayList<SteamServerCallback> _serverCallbacks =
    new CopyOnWriteArrayList<>();
  private static final CopyOnWriteArrayList<MicroTxnCallback> _microTxnCallbacks =
    new CopyOnWriteArrayList<>();
}
