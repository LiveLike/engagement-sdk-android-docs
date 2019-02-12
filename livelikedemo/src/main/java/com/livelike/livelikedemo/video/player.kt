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
    private lateinit var player : SimpleExoPlayer
    private lateinit var mediaSource : MediaSource
    private var playerState = PlayerState()

    private fun initializePlayer(uri: Uri) {
        playerView.requestFocus()

        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector()).also { playerView.player = it }

        mediaSource = buildMediaSource(uri)
        player.repeatMode = Player.REPEAT_MODE_ALL
    }

    private fun buildMediaSource(uri: Uri): ExtractorMediaSource {
        return ExtractorMediaSource.Factory(
            DefaultDataSourceFactory(context, "LLDemoApp")).createMediaSource(uri)
    }

    override fun playMedia(uri: Uri) {
        initializePlayer(uri)
        player.playWhenReady = true
    }

    override fun start() {
        player.prepare(mediaSource)
        with(playerState) {
            player.playWhenReady = whenReady
            player.seekTo(window, position)
        }
    }

    override fun stop() {
        with(player) {
            with(playerState) {
                position = currentPosition
                window = currentWindowIndex
                whenReady = playWhenReady
            }
            stop()
        }
    }

    override fun release() {
        player.release()
    }

    override fun getCurrentPosition(): Long {
        return player.currentPosition
    }
}

interface VideoPlayer {
    fun playMedia(uri: Uri)
    fun start()
    fun stop()
    fun release()
    fun getCurrentPosition() : Long
}