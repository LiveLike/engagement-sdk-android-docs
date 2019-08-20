package com.livelike.livelikesdk.utils

import android.os.Build
import com.livelike.livelikesdk.BuildConfig
import okhttp3.Request

private val userAgent = "Android/${BuildConfig.SDK_VERSION} ${Build.MODEL}/${Build.VERSION.SDK_INT}"
fun Request.Builder.addUserAgent(): Request.Builder {
    return addHeader("User-Agent", userAgent)
        .addHeader("Authorization", "Bearer zhqy1pwSVKXKTQQ_3jhbz92Nxn5ZNE68HcOnV7n55Ar0C2uhmwm7Dw") // TODO: Remove this when user profile are implemented.
}
