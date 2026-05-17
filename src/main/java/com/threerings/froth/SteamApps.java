package com.threerings.froth;

import java.util.concurrent.CopyOnWriteArrayList;

public class SteamApps
{
  public interface DlcInstalledCallback
  {
    void dlcInstalled (int appId);
  }

  public static void addDlcInstalledCallback (DlcInstalledCallback callback) {
    _dlcCbs.add(callback);
  }
  public static void removeDlcInstalledCallback (DlcInstalledCallback callback) {
    _dlcCbs.remove(callback);
  }

  public static String getCurrentGameLanguage () { return "english"; }

  public static boolean isDlcInstalled (int appId) { return false; }

  private static final CopyOnWriteArrayList<DlcInstalledCallback> _dlcCbs =
    new CopyOnWriteArrayList<>();
}
