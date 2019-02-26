package com.livelike.player_preintegrators

import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.livelike.livelikesdk.LiveLikeContentSessionImpl
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.messaging.EpochTime

fun LiveLikeSDK.createExoplayerSession(
    player: SimpleExoPlayer?,
    contentSessionId: String
): LiveLikeContentSessionImpl {
    return this.createContentSession(contentSessionId) {
        val position = player?.currentPosition
        val currentManifest = player?.currentManifest as HlsManifest?
        if (position != null) {
            if (currentManifest?.mediaPlaylist?.hasProgramDateTime!!) {
                val currentAbsoluteTimeMs = currentManifest.mediaPlaylist.startTimeUs / 1000 + position
                EpochTime(currentAbsoluteTimeMs)
            }
            EpochTime(position) // VOD or no PDT
        }
        EpochTime(0) // No time information in this stream
    }
}




