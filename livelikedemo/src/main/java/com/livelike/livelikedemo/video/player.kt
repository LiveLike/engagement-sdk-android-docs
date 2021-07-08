package com.livelike.livelikedemo.video

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.MediaItem

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

class ExoPlayerImpl(private val context: Context, private val playerView: PlayerView) :
    VideoPlayer {

    /*private var player: SimpleExoPlayer? =
        ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())
            .also { playerView.player = it }*/
    private var player: SimpleExoPlayer? = SimpleExoPlayer.Builder(context).build().also { playerView.player = it }
    private var mediaSource: MediaSource = buildMediaSource(Uri.EMPTY)
    private var playerState = PlayerState()


    /** initialization of exoplayer with provided media source */
    private fun initializePlayer(uri: Uri, state: PlayerState, useHls: Boolean = true) {
        playerView.requestFocus()

        /** here exoplayer instance was getting created each time, so a check has been added if the instance of player
        has not been created before then only instantiation is needed else not */
        if(player == null) {
            player = SimpleExoPlayer.Builder(context).build().also { playerView.player = it }
        }

        mediaSource = if (useHls) buildHLSMediaSource(uri) else buildMediaSource(uri)
        playerState = state
        player?.setMediaSource(mediaSource)
        player?.prepare()
        //player?.prepare(mediaSource)
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
            DefaultDataSourceFactory(context, "LLDemoApp")
        ).createMediaSource(MediaItem.fromUri(uri))
    }

    /** responsible for building media sources, the player needs media source instance to
     * play the content */
    private fun buildHLSMediaSource(uri: Uri): HlsMediaSource {
        return HlsMediaSource.Factory(DefaultDataSourceFactory(context, "LLDemoApp"))
            .createMediaSource(MediaItem.fromUri(uri))
    }

    override fun playMedia(uri: Uri, startState: PlayerState) {
        initializePlayer(uri, startState)
    }


    /** responsible for starting the player, with the media source provided */
    override fun start() {
        player?.setMediaSource(mediaSource)
        player?.prepare()
        //player?.prepare(mediaSource)
        player?.playWhenReady = true
        player?.seekToDefaultPosition()
    }


    /** responsible for stopping the player */
    override fun stop() {
        with(playerState) {
            position = player?.currentPosition ?: 0
            window = player?.currentWindowIndex ?: 0
            whenReady = false
        }
        player?.playWhenReady = false
        player?.stop()
    }


    /** responsible for stopping the player and releasing it */
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
