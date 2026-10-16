package com.omenight.bright.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

sealed interface NetworkStatus {
    data object Offline : NetworkStatus
    data object Ethernet : NetworkStatus

    /** [bars] is 0 to 3. */
    data class Wifi(val bars: Int) : NetworkStatus

    /** Anything else that still has internet, such as a USB modem. */
    data object Other : NetworkStatus
}

class NetworkMonitor(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)

    fun status(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(classify(capabilities))
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.Offline)
            }
        }
        trySend(connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.let(::classify) ?: NetworkStatus.Offline)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivity.registerDefaultNetworkCallback(callback)
        } else {
            // Android 6 has no default-network callback, any internet network is close enough on a TV
            val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivity.registerNetworkCallback(request, callback)
        }
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun classify(capabilities: NetworkCapabilities): NetworkStatus = when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkStatus.Ethernet
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkStatus.Wifi(bars(wifiRssi(capabilities)))
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkStatus.Other
        else -> NetworkStatus.Offline
    }

    private fun wifiRssi(capabilities: NetworkCapabilities): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            capabilities.signalStrength
        } else {
            @Suppress("DEPRECATION")
            wifi.connectionInfo.rssi
        }

    // Thresholds follow the usual Android TV status bar buckets
    private fun bars(rssi: Int): Int = when {
        rssi >= -55 -> 3
        rssi >= -67 -> 2
        rssi >= -78 -> 1
        else -> 0
    }
}
