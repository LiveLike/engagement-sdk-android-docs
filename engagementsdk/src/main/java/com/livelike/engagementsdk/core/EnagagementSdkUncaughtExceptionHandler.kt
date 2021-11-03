package com.livelike.engagementsdk.core

import com.livelike.engagementsdk.BuildConfig

internal object EnagagementSdkUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {

    var defaultHandler: Thread.UncaughtExceptionHandler =
        Thread.getDefaultUncaughtExceptionHandler() ?: Thread.UncaughtExceptionHandler { _, _ ->
            /**
             * no op, we can only get here if the original UncaughtExceptionHandler was null
             * which is impossible. Right?
             */
        }

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread?, throwable: Throwable?) {

        var record = false

        throwable?.let {
            record = doContainsSDKFootprint(throwable)
        }

        if (record) {
            throwable?.run {
                throwable.printStackTrace()
            }
        }

        thread?.let {
            throwable?.let {
                defaultHandler.uncaughtException(thread, throwable)
            }
        }
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
        } while (cause != null && count < 10)

        if (count == 10) {
            return true // to make it more fail-safe if there is weird deep connections or repetition in causes then will capture for now to see
        }
        return false
    }
}
