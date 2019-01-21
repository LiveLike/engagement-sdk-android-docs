package com.livelike.livelikedemo.video

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

class ExoPlayerImpl(private val context: Context, private val playerView : PlayerView) : VideoPlayer{
    private lateinit var player : SimpleExoPlayer

    private fun initializePlayer(uri: Uri) {
        playerView.requestFocus()

        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector()).also { playerView.player = it }

        val mediaSource = buildMediaSource(uri)
        player.prepare(mediaSource)
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
        player.playWhenReady = true
    }

    override fun stop() {
        player.stop()
    }

    override fun release() {
        player.release()
    }
}

interface VideoPlayer {
    fun playMedia(uri: Uri)
    fun start()
    fun stop()
    fun release()
}