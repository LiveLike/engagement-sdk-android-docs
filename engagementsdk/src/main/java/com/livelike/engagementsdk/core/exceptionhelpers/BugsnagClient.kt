package com.livelike.engagementsdk.core.exceptionhelpers

import android.app.Application
import android.content.Context
import com.bugsnag.android.Client
import com.livelike.engagementsdk.BUGSNAG_ENGAGEMENT_SDK_KEY

internal object BugsnagClient {

    var client: Client? = null

    fun wouldInitializeBugsnagClient(applicationContext: Context) {
        if (applicationContext is Application) {
            if (client == null) {
                client =
                    Client(applicationContext, BUGSNAG_ENGAGEMENT_SDK_KEY, false)
                // pass false in constructor for disabling bugsnag global handler this can help in cases where host is also using bugsnag.
            }
        } else {

            throw IllegalArgumentException("Pass application context in SDK initialization")
        }
    }
}
