package com.livelike.livelikedemo

import android.app.Application
import com.google.android.exoplayer2.ui.PlayerView
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.video.ExoPlayerImpl
import com.livelike.livelikedemo.video.VideoPlayer
import com.livelike.livelikesdk.LiveLikeSDK

class LiveLikeApplication : Application() {

    companion object {
        const val TEST_CONFIG_URL =
            "https://livelike-webs.s3.amazonaws.com/mobile-pilot/video-backend-sdk-android-with-id.json"
    }

    lateinit var channelManager: ChannelManager
    lateinit var player: VideoPlayer
    private var session: LiveLikeContentSession? = null
    var sdk: LiveLikeSDK? = null

    override fun onCreate() {
        super.onCreate()
        channelManager = ChannelManager(TEST_CONFIG_URL, applicationContext)
        sdk = LiveLikeSDK(getString(R.string.app_id), applicationContext)
    }

    fun createPlayer(playerView: PlayerView): VideoPlayer {
        player = ExoPlayerImpl(baseContext, playerView)
        return player
    }

    fun createSession(sessionId: String): LiveLikeContentSession {
        if (session == null || session?.contentSessionId() != sessionId) {
            session?.close()
            session = sdk?.createContentSession(sessionId, object : LiveLikeSDK.TimecodeGetter {
                override fun getTimecode(): EpochTime {
                    return EpochTime(player.getPDT())
                }
            })
        }
        return session as LiveLikeContentSession
    }
}