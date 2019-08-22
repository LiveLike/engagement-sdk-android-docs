package com.livelike.livelikesdk.utils

import android.os.Build
import com.livelike.livelikesdk.BuildConfig
import com.livelike.livelikesdk.data.repository.UserRepository
import okhttp3.Request

private val userAgent = "Android/${BuildConfig.SDK_VERSION} ${Build.MODEL}/${Build.VERSION.SDK_INT}"

fun Request.Builder.addUserAgent(): Request.Builder {
    return addHeader("User-Agent", userAgent)
}

fun Request.Builder.addAuthorizationBearer(accessToken: String? = null): Request.Builder {
    return addHeader("Authorization", "Bearer ${accessToken ?: UserRepository.userAccessToken}")
}
