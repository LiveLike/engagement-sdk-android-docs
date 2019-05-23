package com.livelike.livelikedemo

import android.app.Application
import com.google.android.exoplayer2.ui.PlayerView
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.video.ExoPlayerImpl
import com.livelike.livelikedemo.video.VideoPlayer
import com.livelike.livelikesdk.LiveLikeSDK

class LiveLikeApplication : Application() {

    companion object {
        const val TEST_CONFIG_URL =
            "https://livelike-webs.s3.amazonaws.com/mobile-pilot/video-backend-sdk-android.json"
    }

    lateinit var channelManager: ChannelManager
    lateinit var player: VideoPlayer
    var session: LiveLikeContentSession? = null

    override fun onCreate() {
        super.onCreate()
        channelManager = ChannelManager(TEST_CONFIG_URL, applicationContext)
    }

    fun createPlayer(playerView: PlayerView): VideoPlayer {
        player = ExoPlayerImpl(baseContext, playerView)
        return player
    }

    fun createSession(sessionId: String, sdk: LiveLikeSDK): LiveLikeContentSession {
        session = player.createSession(sessionId, sdk)
        return session as LiveLikeContentSession
    }
}