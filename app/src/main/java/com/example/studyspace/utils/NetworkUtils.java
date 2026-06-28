package com.example.studyspace.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * NetworkUtils — Connectivity Helper
 *
 * Tutorial Reference: T07 — Connectivity
 * Checks whether the device has an active internet connection.
 * Used before any Firebase (online) operations to gracefully handle offline scenarios.
 */
public class NetworkUtils {

    /**
     * Returns true if the device is currently connected to the internet.
     *
     * On Android 10+ (API 29+) we use NetworkCapabilities for a modern check.
     * On older devices, we fall back to the deprecated activeNetworkInfo.
     *
     * @param context Application or Activity context
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        // Modern API (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) return false;

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true;
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return true;
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return true;
            return false;
        } else {
            // Legacy API for older devices
            @SuppressWarnings("deprecation")
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }
}
