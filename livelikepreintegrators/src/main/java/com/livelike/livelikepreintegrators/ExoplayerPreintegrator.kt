package com.livelike.livelikepreintegrators

import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.messaging.EpochTime

fun LiveLikeSDK.createExoplayerSession(
    player: () -> SimpleExoPlayer?,
    contentId: String,
    sessionReady: (LiveLikeContentSession) -> Unit
) {
    return this.createContentSession(contentId, {
        getExoplayerPdtTime(player)
    }, sessionReady,
        null)
}

fun getExoplayerPdtTime(player: () -> SimpleExoPlayer?): EpochTime {
    return EpochTime(player()?.let {
        it.currentTimeline?.run {
            if (!isEmpty) {
                getWindow(it.currentWindowIndex, Timeline.Window()).windowStartTimeMs + it.currentPosition
            } else {
                it.currentPosition
            }
        }
    } ?: 0)
}
