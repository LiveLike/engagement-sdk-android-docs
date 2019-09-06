package com.livelike.engagementsdk

import com.livelike.engagementsdk.utils.logError
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * A fixed moment in time with a specified baseline and precision.
 *
 * @property timeSinceEpochInMs Number of milliseconds that have elapsed since 00:00:00, 1 January 1970 UTC
 */
class EpochTime(val timeSinceEpochInMs: Long) : Comparable<EpochTime> {
    override fun compareTo(other: EpochTime): Int {
        return timeSinceEpochInMs.compareTo(other.timeSinceEpochInMs)
    }

    operator fun minus(et: EpochTime) =
        EpochTime(timeSinceEpochInMs - et.timeSinceEpochInMs)

    operator fun minus(timeStamp: Long) =
        EpochTime(timeSinceEpochInMs - timeStamp)

    operator fun plus(et: EpochTime) =
        EpochTime(timeSinceEpochInMs + et.timeSinceEpochInMs)

    operator fun plus(timeStamp: Long) =
        EpochTime(timeSinceEpochInMs + timeStamp)
}

internal fun String.parseISO8601(): ZonedDateTime? {
    return try {
        if (isEmpty()) null else ZonedDateTime.parse(
            this,
            DateTimeFormatter.ISO_DATE_TIME
        )
    } catch (exception: org.threeten.bp.format.DateTimeParseException) {
        logError { "Failed to parse Date due to : $exception" }
        null
    }
}

internal fun ZonedDateTime.formatIso8601(): String {
    return DateTimeFormatter.ISO_DATE_TIME.format(this)
}
