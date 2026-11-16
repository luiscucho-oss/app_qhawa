package com.cucho.qhawa.data.environment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.IOException
import java.net.InetAddress
import javax.net.SocketFactory

internal fun Context.canAccessPrototypeNetwork(): Boolean =
    Build.VERSION.SDK_INT < 37 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED

/** Bind the MQTT socket to Wi-Fi even when Android keeps cellular as the default network. */
@Suppress("DEPRECATION") // The default network can omit a Wi-Fi AP with no Internet access.
internal fun Context.prototypeSocketFactory(): SocketFactory {
    if (!canAccessPrototypeNetwork()) throw IOException("Permite el acceso a la red local para recibir lecturas.")
    val connectivity = getSystemService(ConnectivityManager::class.java)
    val address = InetAddress.getByName(QhawaSensorMqtt.HOST)
    val network = connectivity.allNetworks.firstOrNull { network ->
        connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true &&
            connectivity.getLinkProperties(network)?.routes?.any { !it.isDefaultRoute && it.matches(address) } == true
    }
    return network?.socketFactory ?: throw IOException("Conéctate al Wi‑Fi ${QhawaSensorMqtt.WIFI} para recibir lecturas.")
}
