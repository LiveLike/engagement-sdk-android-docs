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
import com.livelike.livelikepreintegrators.createExoplayerSession
import com.livelike.livelikepreintegrators.getExoplayerPdtTime
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.LiveLikeSDK
import java.net.URL

data class PlayerState(var window: Int = 0,
                       var position: Long = 0,
                       var whenReady: Boolean = true)

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
        return getExoplayerPdtTime { getPlayer() }.timeSinceEpochInMs
    }

    private fun buildMediaSource(uri: Uri): HlsMediaSource {
        return HlsMediaSource.Factory(
            DefaultDataSourceFactory(context, "LLDemoApp")).createMediaSource(uri)
    }

    override fun createSession(sessionId: String, sdk: LiveLikeSDK, sessionReady: (LiveLikeContentSession) -> Unit) {
        return sdk.createExoplayerSession(
            { getPlayer() },
            sessionId,
            sessionReady
        )
    }

    private fun getPlayer(): SimpleExoPlayer? {
        return player
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
        player?.release()
        playerState = PlayerState()
    }

    override fun position() : Long {
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
    fun position() : Long
    fun createSession(sessionId: String, sdk: LiveLikeSDK, sessionReady: (LiveLikeContentSession) -> Unit)
    fun getPDT(): Long
}


/*
Class Representing a Demo App channel which comes from service via json like:
	{
      "name": "Android Demo Channel 1",
      "video_url": "http://livecut-streams.livelikecdn.com/live/colorbars-angle1/index.m3u8",
      "video_thumbnail_url": "http://lorempixel.com/200/200/?2",
      "livelike_program_url": "https://livelike-blast.herokuapp.com/api/v1/programs/00f4cdfd-6a19-4853-9c21-51aa46d070a0/"
    }
}*/

data class Channel(val name: String, val video: URL, val thumbnail: URL, val llProgram: URL)
