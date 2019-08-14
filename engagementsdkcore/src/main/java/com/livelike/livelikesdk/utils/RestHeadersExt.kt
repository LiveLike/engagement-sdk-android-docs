package com.livelike.livelikesdk.utils

import android.os.Build
import com.livelike.livelikesdk.BuildConfig
import okhttp3.Request

private val userAgent = "Android/${BuildConfig.SDK_VERSION} ${Build.MODEL}/${Build.VERSION.SDK_INT}"
fun Request.Builder.addUserAgent(): Request.Builder {
    return addHeader("User-Agent", userAgent)
        .addHeader("Authorization", "Bearer h2L4bV5pt12KHQrVa1IApxL6JA9_p9FUmGuj450Be24mpXjx4rkBqA") // TODO: Remove this when user profile are implemented.
}
