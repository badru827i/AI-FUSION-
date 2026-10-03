package com.aifusion.app.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

enum class NetworkType { WIFI, FIVE_G, FOUR_G, OTHER, OFFLINE }

data class NetworkState(val type: NetworkType, val connected: Boolean, val metered: Boolean)

object NetworkGuardian {
    fun state(context: Context): NetworkState {
        val cm = context.getSystemService(ConnectivityManager::class.java)
            ?: return NetworkState(NetworkType.OFFLINE, false, true)
        val network = cm.activeNetwork ?: return NetworkState(NetworkType.OFFLINE, false, true)
        val caps = cm.getNetworkCapabilities(network)
            ?: return NetworkState(NetworkType.OFFLINE, false, true)
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NR) -> NetworkType.FIVE_G
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.FOUR_G
            else -> NetworkType.OTHER
        }
        val metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return NetworkState(type, true, metered)
    }

    fun label(state: NetworkState): String = when (state.type) {
        NetworkType.WIFI -> "Wi-Fi"
        NetworkType.FIVE_G -> "5G"
        NetworkType.FOUR_G -> "4G"
        NetworkType.OTHER -> "Online"
        NetworkType.OFFLINE -> "Offline"
    }
}