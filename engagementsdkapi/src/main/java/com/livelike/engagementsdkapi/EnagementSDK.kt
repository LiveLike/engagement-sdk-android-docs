package com.livelike.engagementsdkapi

class EpochTime(val timeSinceEpochInMs: Long) : Comparable<EpochTime> {
    override fun compareTo(other: EpochTime): Int {
        return timeSinceEpochInMs.compareTo(other.timeSinceEpochInMs)
    }

    operator fun minus(et: EpochTime) = EpochTime(timeSinceEpochInMs - et.timeSinceEpochInMs)
    operator fun minus(timeStamp: Long) = EpochTime(timeSinceEpochInMs - timeStamp)
}
