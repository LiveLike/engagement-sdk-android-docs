package com.livelike.livelikepreintegrators

import android.util.Log
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.messaging.EpochTime

fun LiveLikeSDK.createExoplayerSession(
    player: () -> SimpleExoPlayer?,
    contentId: String,
    sessionReady: (LiveLikeContentSession) -> Unit
) {
    return this.createContentSession(contentId, {
        val position = player()?.currentPosition
        if (position != null) {
            val currentManifest = player()?.currentManifest as HlsManifest?
            if (currentManifest?.mediaPlaylist?.hasProgramDateTime != null && currentManifest.mediaPlaylist?.hasProgramDateTime!!) {
                val currentAbsoluteTimeMs = currentManifest.mediaPlaylist.startTimeUs / 1000 + position
                Log.i("Sync", "currentAbsoluteTimeMs $currentAbsoluteTimeMs")
                EpochTime(currentAbsoluteTimeMs)
            } else {
                Log.i("Sync", "position $position")
                EpochTime(position) // VOD or no PDT
            }
        } else {
            Log.i("Sync", "noo... ${player() == null}")
            EpochTime(0) // No time information in this stream
        }
    }, sessionReady)
}




