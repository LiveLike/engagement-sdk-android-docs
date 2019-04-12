package com.livelike.livelikepreintegrators

import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.LiveLikeSDK

fun LiveLikeSDK.createExoplayerSession(
    player: () -> SimpleExoPlayer?,
    contentId: String
): LiveLikeContentSession {
    return this.createContentSession(contentId) {
        getExoplayerPdtTime(player)
    }
}

fun getExoplayerPdtTime(player: () -> SimpleExoPlayer?): Long {
    return player()?.let {
        it.currentTimeline?.run {
            if (!isEmpty) {
                getWindow(it.currentWindowIndex, Timeline.Window()).windowStartTimeMs + it.currentPosition
            } else {
                it.currentPosition
            }
        }
    } ?: 0
}
