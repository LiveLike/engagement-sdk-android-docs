package com.livelike.livelikedemo.video

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
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
            player?.seekTo(window, position)
            player?.repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    private fun buildMediaSource(uri: Uri): ExtractorMediaSource {
        return ExtractorMediaSource.Factory(
            DefaultDataSourceFactory(context, "LLDemoApp")).createMediaSource(uri)
    }

    override fun playMedia(uri: Uri, startPosition: Long, playWhenReady: Boolean) {
        initializePlayer(uri, PlayerState(0, startPosition, playWhenReady))
    }

    override fun start() {
        player?.prepare(mediaSource)
        with(playerState) {
            player?.playWhenReady = true
            player?.seekTo(window, position)
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
}