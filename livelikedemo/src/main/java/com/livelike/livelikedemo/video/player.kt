package com.livelike.livelikedemo.video

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.livelike.livelikepreintegrators.PlayerProvider
import com.livelike.livelikepreintegrators.getExoplayerPdtTime

data class PlayerState(
    var window: Int = 0,
    var position: Long = 0,
    var whenReady: Boolean = true
)

class ExoPlayerImpl(private val context: Context, private val playerView: PlayerView) : VideoPlayer {

    private var player: SimpleExoPlayer? =
        ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector()).also { playerView.player = it }
    private var mediaSource: MediaSource = buildMediaSource(Uri.EMPTY)
    private var playerState = PlayerState()

    private fun initializePlayer(uri: Uri, state: PlayerState, useHls: Boolean = true) {
        playerView.requestFocus()

        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector()).also { playerView.player = it }

        mediaSource = if (useHls) buildHLSMediaSource(uri) else buildMediaSource(uri)
        playerState = state
        player?.prepare(mediaSource)
        with(playerState) {
            player?.playWhenReady = whenReady
            player?.seekToDefaultPosition()
            player?.repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    override fun getPDT(): Long {
        return getExoplayerPdtTime(object : PlayerProvider {
            override fun get(): SimpleExoPlayer? {
                return player
            }
        })
    }

    private fun buildMediaSource(uri: Uri): HlsMediaSource {
        return HlsMediaSource.Factory(
            DefaultDataSourceFactory(context, "LLDemoApp")).createMediaSource(uri)
    }

    private fun buildHLSMediaSource(uri: Uri): HlsMediaSource {
        return HlsMediaSource.Factory(DefaultDataSourceFactory(context, "LLDemoApp")).createMediaSource(uri)
    }

    override fun playMedia(uri: Uri, startState: PlayerState) {
        initializePlayer(uri, startState)
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
        player?.stop()
        player?.release()
        player?.setVideoSurfaceHolder(null)
        player = null
        playerState = PlayerState()
    }

    override fun position(): Long {
        return player?.currentPosition ?: 0
    }

    override fun seekTo(position: Long) {
        player?.seekTo(position)
    }
}

interface VideoPlayer {
    fun playMedia(uri: Uri, startState: PlayerState = PlayerState())
    fun start()
    fun stop()
    fun seekTo(position: Long)
    fun release()
    fun position(): Long
    fun getPDT(): Long
}
