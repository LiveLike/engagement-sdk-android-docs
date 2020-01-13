package com.livelike.engagementsdk.core

import com.livelike.engagementsdk.BuildConfig
import com.livelike.engagementsdk.core.exceptionhelpers.BugsnagClient.client

/**
 * It is like Global exception handler for our sdk running on host process :)
 * We capture every crash here on all threads except main which do not have catch block or unhandled.
 * we filter out on the basis of sdk package name and propogate back the remaining to root handler.
 */

internal object EnagagementSdkUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {

    var defaultHandler: Thread.UncaughtExceptionHandler =
        Thread.getDefaultUncaughtExceptionHandler()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread?, throwable: Throwable?) {

//        var swallow = true // should swallow the exception inorder to prevent the crash
        var record = false // should record exception if it possibility is due to SDK.

        throwable?.let { throwable ->

            record = doContainsSDKFootprint(throwable)

//            if (p0?.name == "main" || !record) {
//                swallow = false
//                return@let
//            }
        }

        if (record) {
            throwable?.run {
                client?.notify(throwable)
                throwable.printStackTrace()
            }
        }

        //        if (!swallow) {
        defaultHandler.uncaughtException(thread, throwable)
//        }
    }

    private fun doContainsSDKFootprint(throwable: Throwable): Boolean {
        var cause: Throwable? = throwable
        var count = 0
        do {
            cause?.let { currentCause ->
                for (s in currentCause.stackTrace) {
                    if (s.className.contains(BuildConfig.LIBRARY_PACKAGE_NAME)) {
                        return true
                    }
                }
                cause = currentCause.cause
            }
            count++
        } while (cause != null && count <10)

        if (count == 10) {
            return true // to make it more fail-safe if there is weird deep connections or repetition in causes then will capture for now to see
        }
        return false
    }
}
