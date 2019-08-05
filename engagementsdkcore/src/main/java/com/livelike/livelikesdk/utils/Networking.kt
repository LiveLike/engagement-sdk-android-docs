package com.livelike.livelikesdk.utils

import android.os.Build
import com.livelike.livelikesdk.BuildConfig
import okhttp3.Request

fun Request.Builder.addUserAgent(): Request.Builder {
    val sdkPlatform = "Android"
    val sdkVersion = BuildConfig.SDK_VERSION
    val deviceModel = Build.MODEL
    val deviceVersion = Build.VERSION.SDK_INT
    return addHeader("User-Agent", "$sdkPlatform/$sdkVersion $deviceModel/$deviceVersion")
}
