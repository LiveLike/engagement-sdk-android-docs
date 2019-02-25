package com.livelike.player_preintegrators

import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsManifest

class ExoplayerPreintegrator(private val player: SimpleExoPlayer?) {
    fun getCurrentDate(): Long {
        val position = player?.currentPosition
        val currentManifest = player?.currentManifest as HlsManifest?
        if (position != null) {
            if (currentManifest?.mediaPlaylist?.hasProgramDateTime!!) {
                val currentAbsoluteTimeMs = currentManifest.mediaPlaylist.startTimeUs / 1000 + position
                return currentAbsoluteTimeMs
            }
            return position // VOD or no PDT
        }
        return 0 // No time information in this stream
    }
}