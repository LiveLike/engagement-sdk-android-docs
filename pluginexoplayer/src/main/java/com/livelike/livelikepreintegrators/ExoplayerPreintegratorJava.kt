package com.livelike.livelikepreintegrators

import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.LiveLikeSDK

/**
 * This extension act as a plugin for Exoplayer.
 * It retrieves the timecode from an HLS stream using the PDT tags.
 *
 * @param playerGetter An interface returning the latest player instance when called.
 * @param programId The program to connect the session with. This Id can be created from the Engagement CMS.
 */
fun LiveLikeSDK.createExoplayerSession(
    playerGetter: PlayerGetter,
    programId: String
): LiveLikeContentSession {
    return this.createContentSession(programId) {
        getExoplayerPdtTime(playerGetter)
    }
}

fun getExoplayerPdtTime(playerGetter: PlayerGetter): Long {
    return playerGetter.getPlayer()?.let {
        it.currentTimeline?.run {
            if (!isEmpty) {
                getWindow(it.currentWindowIndex, Timeline.Window()).windowStartTimeMs + it.currentPosition
            } else {
                it.currentPosition
            }
        }
    } ?: 0
}

interface PlayerGetter {
    fun getPlayer(): SimpleExoPlayer?
}
