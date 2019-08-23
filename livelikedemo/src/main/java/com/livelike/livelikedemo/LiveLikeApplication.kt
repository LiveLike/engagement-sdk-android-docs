package com.livelike.livelikedemo

import android.app.Application
import android.content.Context
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
    lateinit var sdk: EngagementSDK
    lateinit var sdk2: EngagementSDK

    override fun onCreate() {
        super.onCreate()
        Bugsnag.init(this)
        channelManager = ChannelManager(TEST_CONFIG_URL, applicationContext)

        val accessToken = getSharedPreferences("Test_Demo", Context.MODE_PRIVATE).getString(PREF_USER_ACCESS_TOKEN, null)
        sdk = EngagementSDK(getString(R.string.app_id), applicationContext, accessToken)
        if (accessToken == null) {
            fetchAndPersisToken(sdk)
        }

        sdk2 = EngagementSDK("vjiRzT1wPpLEdgQwjWXN0TAuTx1KT7HljjDD4buA", applicationContext)
    }

    private fun fetchAndPersisToken(sdk: EngagementSDK) {
        sdk.userStream.subscribe(javaClass.simpleName) {
            it?.let {
                sdk.userStream.unsubscribe(javaClass.simpleName)
                getSharedPreferences("Test_Demo", Context.MODE_PRIVATE).edit().putString(
                    PREF_USER_ACCESS_TOKEN, it.accessToken).apply()
            }
        }
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
