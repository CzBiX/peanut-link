package com.czbix.peanutlink;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class MiscUtils {
    private static final String WIFI_NAME = "花生地铁";

    public static String getMacAddress(Context context) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        return wifiInfo.getMacAddress();
    }

    public static boolean isPeanutAp(Context context) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null || wifiInfo.getNetworkId() == -1) {
            return false;
        }

        return wifiInfo.getSSID().contains(WIFI_NAME);
    }
}
