package com.threerings.froth;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.concurrent.CopyOnWriteArrayList;

public class SteamNetworking
{
  public enum P2PSend {
    UNRELIABLE, UNRELIABLE_NO_DELAY, RELIABLE, RELIABLE_WITH_BUFFERING
  }

  public enum P2PSessionError {
    NONE,
    @Deprecated NOT_RUNNING_APP,
    NO_RIGHTS_TO_APP,
    @Deprecated DESTINATION_NOT_LOGGED_IN,
    TIMEOUT,
    ;
  }

  public interface P2PSessionRequestCallback
  {
    void p2pSessionRequest (long steamIdRemote);
  }

  public interface P2PSessionConnectCallback
  {
    void p2pSessionConnectFail (long steamIdRemote, P2PSessionError error);
  }

  public static final int MAX_UNRELIABLE_SIZE = 1200;
  public static final int MAX_RELIABLE_SIZE = 1048576;

  public static void addSessionRequestCallback (P2PSessionRequestCallback callback) {
    _reqCbs.add(callback);
  }
  public static void removeSessionRequestCallback (P2PSessionRequestCallback callback) {
    _reqCbs.remove(callback);
  }
  public static void addSessionConnectCallback (P2PSessionConnectCallback callback) {
    _connCbs.add(callback);
  }
  public static void removeSessionConnectCallback (P2PSessionConnectCallback callback) {
    _connCbs.remove(callback);
  }

  public static boolean sendP2PPacket (
      long steamIdRemote, ByteBuffer data, P2PSend sendType, int channel) {
    return false;
  }

  public static boolean isP2PPacketAvailable (IntBuffer msgSize, int channel) {
    return false;
  }

  public static boolean readP2PPacket (
      ByteBuffer dest, LongBuffer steamIdRemote, int channel) {
    return false;
  }

  public static boolean acceptP2PSessionWithUser (long steamIdRemote) { return false; }
  public static boolean closeP2PSessionWithUser (long steamIdRemote) { return false; }
  public static boolean closeP2PChannelWithUser (long steamIdRemote, int channel) { return false; }

  private static final CopyOnWriteArrayList<P2PSessionRequestCallback> _reqCbs =
    new CopyOnWriteArrayList<>();
  private static final CopyOnWriteArrayList<P2PSessionConnectCallback> _connCbs =
    new CopyOnWriteArrayList<>();
}
