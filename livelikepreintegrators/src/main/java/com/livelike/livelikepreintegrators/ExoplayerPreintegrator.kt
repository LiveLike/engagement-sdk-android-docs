package com.livelike.livelikepreintegrators

import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.messaging.EpochTime

fun LiveLikeSDK.createExoplayerSession(
    player: SimpleExoPlayer,
    contentId: String,
    sessionReady: (LiveLikeContentSession) -> Unit
) {
    return this.createContentSession(contentId, {
        val position = player.currentPosition
        val currentManifest = player.currentManifest as HlsManifest?
        if (currentManifest?.mediaPlaylist?.hasProgramDateTime!!) {
            val currentAbsoluteTimeMs = currentManifest.mediaPlaylist.startTimeUs / 1000 + position
            EpochTime(currentAbsoluteTimeMs)
        }
        EpochTime(position) // VOD or no PDT
        EpochTime(0) // No time information in this stream
    }, sessionReady)
}




