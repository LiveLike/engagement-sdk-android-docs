package com.livelike.livelikedemo

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.google.android.exoplayer2.ui.PlayerView
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.video.ExoPlayerImpl
import com.livelike.livelikedemo.video.VideoPlayer
import com.livelike.livelikesdk.EngagementSDK
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.services.messaging.proxies.WidgetInterceptor

class LiveLikeApplication : Application() {

    companion object {
        const val TEST_CONFIG_URL =
            "https://livelike-webs.s3.amazonaws.com/mobile-pilot/video-backend-sdk-android-with-id.json"
    }

    lateinit var channelManager: ChannelManager
    lateinit var player: VideoPlayer
    private var session: LiveLikeContentSession? = null
    var sdk: EngagementSDK? = null
    var sdk2: EngagementSDK? = null

    override fun onCreate() {
        super.onCreate()
        Bugsnag.init(this)
        channelManager = ChannelManager(TEST_CONFIG_URL, applicationContext)
        sdk = EngagementSDK(getString(R.string.app_id), applicationContext)
        sdk2 = EngagementSDK("vjiRzT1wPpLEdgQwjWXN0TAuTx1KT7HljjDD4buA", applicationContext)
    }

    fun createPlayer(playerView: PlayerView): VideoPlayer {
        player = ExoPlayerImpl(baseContext, playerView)
        return player
    }

    fun createSession(sessionId: String, widgetInterceptor: WidgetInterceptor): LiveLikeContentSession {
        if (session == null || session?.contentSessionId() != sessionId) {
            session?.close()
            session = sdk?.createContentSession(sessionId, object : EngagementSDK.TimecodeGetter {
                override fun getTimecode(): EpochTime {
                    return EpochTime(player.getPDT())
                }
            })
        }
        session!!.widgetInterceptor = widgetInterceptor
        return session as LiveLikeContentSession
    }
}
