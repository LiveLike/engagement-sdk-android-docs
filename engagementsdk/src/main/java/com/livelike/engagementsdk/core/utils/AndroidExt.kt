package com.livelike.engagementsdk.core.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

internal fun Context.scanForActivity(): Activity? {
    if (this is Activity)
        return this
    else if (this is ContextWrapper)
        return this.baseContext.scanForActivity()
    return null
}

fun Context.isNetworkConnected(): Boolean {
    val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        connectivityManager.activeNetwork?.let { network ->
            connectivityManager.getNetworkCapabilities(network)?.let { networkCapabilities ->
                when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                    else -> false
                }
            } ?: false
        } ?: false
    } else {
        @Suppress("DEPRECATION") //kept due to pre M support
        connectivityManager.activeNetworkInfo?.isConnected ?: false
    }
}
