package com.livelike.engagementsdkapi

/**
 * EpochTime represents a timestamp.
 * It is used by the library to synchronize the session's events with the video across all users.
 *
 * @property timeSinceEpochInMs Number of milliseconds that have elapsed since 00:00:00 Thursday, 1 January 1970 UTC
 */
class EpochTime(val timeSinceEpochInMs: Long) : Comparable<EpochTime> {
    override fun compareTo(other: EpochTime): Int {
        return timeSinceEpochInMs.compareTo(other.timeSinceEpochInMs)
    }

    operator fun minus(et: EpochTime) = EpochTime(timeSinceEpochInMs - et.timeSinceEpochInMs)
    operator fun minus(timeStamp: Long) = EpochTime(timeSinceEpochInMs - timeStamp)
}
