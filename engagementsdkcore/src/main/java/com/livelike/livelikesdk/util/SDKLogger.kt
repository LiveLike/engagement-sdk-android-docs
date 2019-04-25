package com.livelike.livelikesdk.util

import android.util.Log
import java.io.IOException

enum class LogLevel(
    val code: Int,
    val logger: (String, String) -> Int,
    val exceptionLogger: (String, String, Throwable) -> Int
) {
    Verbose(Log.VERBOSE, Log::v, Log::v),
    Debug(Log.DEBUG, Log::d, Log::d),
    Info(Log.INFO, Log::i, Log::i),
    Warn(Log.WARN, Log::w, Log::w),
    Error(Log.ERROR, Log::e, Log::e),
    None(Log.ASSERT + 1, { _, _ -> 0 }, { _, _, _ -> 0 })
}


/** The lowest (most granular) log level to log */
var minimumLogLevel: LogLevel = LogLevel.Verbose


internal inline fun <reified T> T.logVerbose(message: () -> Any?) = log(LogLevel.Verbose, message)
internal inline fun <reified T> T.logDebug(message: () -> Any?) = log(LogLevel.Debug, message)
internal inline fun <reified T> T.logInfo(message: () -> Any?) = log(LogLevel.Info, message)
internal inline fun <reified T> T.logWarn(message: () -> Any?) = log(LogLevel.Warn, message)
internal inline fun <reified T> T.logError(message: () -> Any?) = log(LogLevel.Error, message)

private var handler: ((String) -> Unit)? = null

/**
 * Add a log handler to intercept the logs.
 * This can be helpful for debugging.
 *
 * @param logHandler Method processing the log string received.
 */
fun registerLogsHandler(logHandler: (String) -> Unit) {
    handler = logHandler
}


internal inline fun <reified T> T.log(level: LogLevel, message: () -> Any?) {
    if (level >= minimumLogLevel) {
        message().let {
            val tag = T::class.java.name
            when (it) {
                is Throwable -> level.exceptionLogger(tag, it.message ?: "", it)
                is Unit -> Unit
                null -> Unit
                else -> level.logger(tag, it.toString())
            }
        }
        message().let {
            handler?.invoke(it.toString())
        }
    }
}


@Suppress("unused")
private class LoggerSample {
    data class Fruit(val name: String, val qty: Int)

    val threeApples: Fruit? = Fruit("Apple", 3)

    fun basicLogging() {
        minimumLogLevel = LogLevel.Verbose

        logVerbose { "Just an informative message that no-one really cares about" }

        logDebug { "This might be interesting" }

        logInfo { threeApples ?: "no apples" }

        logWarn { RuntimeException("This wasn't supposed to happen but it doesn't matter") }

        logError { IOException("Could not connect to nobody nowhere") }
    }
}