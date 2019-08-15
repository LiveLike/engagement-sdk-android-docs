package com.livelike.livelikesdk.utils

import android.os.Build
import com.livelike.livelikesdk.BuildConfig
import okhttp3.Request

private val userAgent = "Android/${BuildConfig.SDK_VERSION} ${Build.MODEL}/${Build.VERSION.SDK_INT}"
fun Request.Builder.addUserAgent(): Request.Builder {
    return addHeader("User-Agent", userAgent)
        .addHeader("Authorization", "Bearer jNcZgi6JutiVPYHl7uBHxmP6w7DKGBNJvdwIqtjypnCQ71Mi2mt3Eg") // TODO: Remove this when user profile are implemented.
}
