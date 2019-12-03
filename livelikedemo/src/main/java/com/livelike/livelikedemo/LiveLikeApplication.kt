package com.livelike.livelikedemo

import android.app.Application
import android.content.Context
import com.bugsnag.android.Bugsnag
import com.google.android.exoplayer2.ui.PlayerView
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.services.messaging.proxies.WidgetInterceptor
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.video.ExoPlayerImpl
import com.livelike.livelikedemo.video.VideoPlayer

class LiveLikeApplication : Application() {

    companion object {
        const val TEST_CONFIG_URL = BuildConfig.TEST_CONFIG_URL
//            "https://livelike-webs.s3.amazonaws.com/mobile-pilot/video-backend-sdk-android-with-id.json"
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

        val accessToken = getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).getString(PREF_USER_ACCESS_TOKEN, null)
        sdk = EngagementSDK(BuildConfig.APP_CLIENT_ID, applicationContext, accessToken)
        if (accessToken == null) {
            fetchAndPersisToken(sdk)
        }

//        TODO: THIS SHOULD BE FIXED ASAP
//        sdk2 = EngagementSDK("vjiRzT1wPpLEdgQwjWXN0TAuTx1KT7HljjDD4buA", applicationContext)
    }

    private fun fetchAndPersisToken(sdk: EngagementSDK) {
        sdk.userStream.subscribe(javaClass.simpleName) {
            it?.let {
                sdk.userStream.unsubscribe(javaClass.simpleName)
                getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).edit().putString(
                    PREF_USER_ACCESS_TOKEN, it.accessToken).apply()
            }
        }
    }

    fun createPlayer(playerView: PlayerView): VideoPlayer {
        player = ExoPlayerImpl(baseContext, playerView)
        return player
    }

    fun createSession(sessionId: String, widgetInterceptor: WidgetInterceptor? = null): LiveLikeContentSession {
        if (session == null || session?.contentSessionId() != sessionId) {
            session?.close()
            session = sdk.createContentSession(sessionId, object : EngagementSDK.TimecodeGetter {
                override fun getTimecode(): EpochTime {
                    return EpochTime(player.getPDT())
                }
            })
        }
        session!!.widgetInterceptor = widgetInterceptor
        return session as LiveLikeContentSession
    }
}

const val PREFERENCES_APP_ID = BuildConfig.APP_CLIENT_ID + "Test_Demo"
