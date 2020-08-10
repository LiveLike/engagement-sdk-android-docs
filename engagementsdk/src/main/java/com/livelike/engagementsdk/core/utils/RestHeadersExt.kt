package com.livelike.engagementsdk.core.utils

import android.os.Build
import com.livelike.engagementsdk.BuildConfig
import okhttp3.Request

private val userAgent = "Android/${BuildConfig.SDK_VERSION} ${Build.MODEL}/${Build.VERSION.SDK_INT}"

fun Request.Builder.addUserAgent(): Request.Builder {
<<<<<<< Updated upstream:engagementsdk/src/main/java/com/livelike/engagementsdk/core/utils/RestHeadersExt.kt
    return addHeader("User-Agent",
=======
    return addHeader(
        "User-Agent",
>>>>>>> Stashed changes:engagementsdk/src/main/java/com/livelike/engagementsdk/utils/RestHeadersExt.kt
        userAgent
    )
}

fun Request.Builder.addAuthorizationBearer(accessToken: String?): Request.Builder {
    return if (accessToken.isNullOrEmpty()) {
        this
    } else {
        addHeader("Authorization", "Bearer $accessToken")
    }
}
