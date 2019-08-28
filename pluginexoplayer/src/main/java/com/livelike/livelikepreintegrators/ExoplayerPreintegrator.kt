package com.livelike.livelikepreintegrators

import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.LiveLikeContentSession

/**
 * This extension act as a plugin for Exoplayer.
 * It retrieves the timecode from an HLS stream using the PDT tags.
 *
 * @param playerProvider An interface returning the latest player instance when called.
 * @param programId The program to connect the session with. This Id can be created from the Engagement CMS.
 */
fun EngagementSDK.createExoplayerSession(
    playerProvider: PlayerProvider,
    programId: String
): LiveLikeContentSession {
    return this.createContentSession(programId, object : EngagementSDK.TimecodeGetter {
        override fun getTimecode(): EpochTime {
            return EpochTime(getExoplayerPdtTime(playerProvider))
        }
    })
}

fun getExoplayerPdtTime(playerProvider: PlayerProvider): Long {
    return playerProvider.get()?.let {
        it.currentTimeline?.run {
            if (!isEmpty) {
                getWindow(it.currentWindowIndex, Timeline.Window()).windowStartTimeMs + it.currentPosition
            } else {
                it.currentPosition
            }
        }
    } ?: 0
}

interface PlayerProvider {
    fun get(): SimpleExoPlayer?
}
