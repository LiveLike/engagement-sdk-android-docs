package com.livelike.engagementsdk

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
