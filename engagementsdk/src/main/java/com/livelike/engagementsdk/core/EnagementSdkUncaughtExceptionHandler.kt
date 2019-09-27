package com.livelike.engagementsdk.core

import com.livelike.engagementsdk.core.exceptionhelpers.BugsnagClient.client

/**
 * It is like Global exception handler for our sdk running on host process :)
 * We capture every crash here on all threads except main which do not have catch block or unhandled.
 * we filter out on the basis of sdk package name and propogate back the remaining to root handler.
 */

internal object EnagementSdkUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {

    var defaultHandler: Thread.UncaughtExceptionHandler =
        Thread.getDefaultUncaughtExceptionHandler()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(p0: Thread?, p1: Throwable?) {

        var swallow = true // should swallow the exception inorder to prevent the crash
        var record = false // should record exception if it possibility is due to SDK.

        p1?.let { throwable ->

            for (s in throwable.stackTrace) {
                if (s.className.contains("com.livelike.engagementsdk")) {
                    record = true
                }
            }

            if (p0?.name == "main" || throwable is ClassNotFoundException) {
                swallow = false
                return@let
            }
        }

        if (!swallow) {
            defaultHandler.uncaughtException(p0, p1)
        }

        if (record) {
            p1?.run {
                client?.notify(p1)
                p1.printStackTrace()
            }
        }
    }
}
