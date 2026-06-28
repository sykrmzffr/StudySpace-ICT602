package com.example.studyspace.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * NetworkUtils — Connectivity Helper
 *
 * Tutorial Reference: T07 — Connectivity
 * Checks whether the device has an active internet connection.
 * Used before any Firebase (online) operations to gracefully handle offline scenarios.
 */
object NetworkUtils {

    /**
     * Returns true if the device is currently connected to the internet.
     *
     * On Android 10+ (API 29+) we use NetworkCapabilities for a modern check.
     * On older devices, we fall back to the deprecated activeNetworkInfo.
     *
     * @param context Application or Activity context
     */
    fun isConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Modern API (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            // Legacy API for older devices
            @Suppress("DEPRECATION")
            val activeNetwork = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return activeNetwork != null && activeNetwork.isConnected
        }
    }
}
