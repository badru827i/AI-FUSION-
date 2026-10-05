package com.aifusion.app.core

import android.content.Context
import androidx.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager

enum class NetworkType { WIFI, FIVE_G, FOUR_G, OTHER, OFFLINE }

data class NetworkState(val type: NetworkType, val connected: Boolean, val metered: Boolean)

object NetworkGuardian {
    fun state(context: Context): NetworkState {
        val cm = context.getSystemService(ConnectivityManager::class.java)
            ?: return NetworkState(NetworkType.OFFLINE, false, true)
        val network = cm.activeNetwork ?: return NetworkState(NetworkType.OFFLINE, false, true)
        val caps = cm.getNetworkCapabilities(network)
            ?: return NetworkState(NetworkType.OFFLINE, false, true)

        val cellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            cellular && isFiveG(context) -> NetworkType.FIVE_G
            cellular -> NetworkType.FOUR_G
            else -> NetworkType.OTHER
        }

        val metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return NetworkState(type, true, metered)
    }

    @SuppressLint("MissingPermission")
    private fun isFiveG(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val tm = context.getSystemService(TelephonyManager::class.java) ?: return false
        return runCatching {
            tm.dataNetworkType == TelephonyManager.NETWORK_TYPE_NR
        }.getOrDefault(false)
    }

    fun label(state: NetworkState): String = when (state.type) {
        NetworkType.WIFI -> "Wi-Fi"
        NetworkType.FIVE_G -> "5G"
        NetworkType.FOUR_G -> "4G"
        NetworkType.OTHER -> "Online"
        NetworkType.OFFLINE -> "Offline"
    }
}
