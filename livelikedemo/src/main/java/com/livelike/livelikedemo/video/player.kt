package com.livelike.livelikedemo.video

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory


data class PlayerState(var window: Int = 0,
                       var position: Long = 0,
                       var whenReady: Boolean = true)

class ExoPlayerImpl(private val context: Context, private val playerView : PlayerView) : VideoPlayer{


    private var player : SimpleExoPlayer? = null
    private lateinit var mediaSource : MediaSource
    private var playerState = PlayerState()

    private fun initializePlayer(uri: Uri, state: PlayerState) {
        playerView.requestFocus()

        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector()).also { playerView.player = it }

        mediaSource = buildMediaSource(uri)
        playerState = state
        player?.prepare(mediaSource)
        with(playerState) {
            player?.playWhenReady = whenReady
            player?.seekToDefaultPosition()
            player?.repeatMode = Player.REPEAT_MODE_ALL
        }

    }

    private fun buildMediaSource(uri: Uri): HlsMediaSource {
        return HlsMediaSource.Factory(
            DefaultDataSourceFactory(context, "LLDemoApp")).createMediaSource(uri)
    }

    override fun getCurrentDate(): Long {
        val position = player?.currentPosition
        val currentManifest = player?.currentManifest as HlsManifest
        if (position != null) {
            if (currentManifest.mediaPlaylist.hasProgramDateTime) {
                val currentAbsoluteTimeMs = currentManifest.mediaPlaylist.startTimeUs / 1000 + position
                return currentAbsoluteTimeMs
            }
            return position // VOD or no PDT
        }
        return 0 // No time information in this stream
    }

    override fun playMedia(uri: Uri, startPosition: Long, playWhenReady: Boolean) {
        initializePlayer(uri, PlayerState(0, startPosition, playWhenReady))
    }

    override fun start() {
        player?.prepare(mediaSource)
        with(playerState) {
            player?.playWhenReady = true
            player?.seekToDefaultPosition()
        }
    }

    override fun stop() {
        with(playerState) {
            position = player?.currentPosition ?: 0
            window = player?.currentWindowIndex ?: 0
            whenReady = false
        }
        player?.stop()
    }

    override fun release() {
        player?.release()
    }

    override fun position() : Long {
        return player?.currentPosition ?: 0
    }

    override fun seekTo(position: Long) {
        player?.seekTo(position)
    }
}

interface VideoPlayer {
    fun playMedia(uri: Uri, startPosition: Long = 0, playWhenReady: Boolean = true)
    fun start()
    fun stop()
    fun seekTo(position: Long)
    fun release()
    fun position() : Long
    fun getCurrentDate(): Long
}