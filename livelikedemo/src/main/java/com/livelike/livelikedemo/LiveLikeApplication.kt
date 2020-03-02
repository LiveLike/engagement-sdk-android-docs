package com.livelike.livelikedemo

import android.app.Application
import android.content.Context
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.google.android.exoplayer2.ui.PlayerView
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.publicapis.ErrorDelegate
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
    val timecodeGetter = object : EngagementSDK.TimecodeGetter {
        override fun getTimecode(): EpochTime {
            return EpochTime(player.getPDT())
        }
    }
    private var publicSession: LiveLikeContentSession? = null
    private var privateGroupChatsession: LiveLikeContentSession? = null

    lateinit var sdk: EngagementSDK
    lateinit var sdk2: EngagementSDK

    override fun onCreate() {
        super.onCreate()
        Bugsnag.init(this)
        channelManager = ChannelManager(TEST_CONFIG_URL, applicationContext)

        initSDK()
//        TODO: THIS SHOULD BE FIXED ASAP
//        sdk2 = EngagementSDK("vjiRzT1wPpLEdgQwjWXN0TAuTx1KT7HljjDD4buA", applicationContext)
    }

    private fun initSDK() {
        val accessToken = getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).getString(
            PREF_USER_ACCESS_TOKEN,
            null
        )
        sdk = EngagementSDK(
            BuildConfig.APP_CLIENT_ID,
            applicationContext,
            accessToken,
            object : ErrorDelegate() {
                override fun onError(error: String) {
                    android.os.Handler(Looper.getMainLooper()).postDelayed({
                        initSDK()
                    }, 1000)
                }
            })
        if (accessToken == null) {
            fetchAndPersisToken(sdk)
        }
    }

    private fun fetchAndPersisToken(sdk: EngagementSDK) {
        sdk.userStream.subscribe(javaClass.simpleName) {
            it?.let {
                sdk.userStream.unsubscribe(javaClass.simpleName)
                getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).edit().putString(
                    PREF_USER_ACCESS_TOKEN, it.accessToken
                ).apply()
            }
        }
    }

    fun createPlayer(playerView: PlayerView): VideoPlayer {
        player = ExoPlayerImpl(baseContext, playerView)
        return player
    }

    fun removePublicSession() {
        publicSession?.close()
        publicSession = null
    }

    fun removePrivateSession() {
        privateGroupChatsession?.close()
        privateGroupChatsession = null
    }

    fun createPublicSession(
        sessionId: String,
        widgetInterceptor: WidgetInterceptor? = null
    ): LiveLikeContentSession {
        if (publicSession == null || publicSession?.contentSessionId() != sessionId) {
            publicSession?.close()
            publicSession = sdk.createContentSession(sessionId, timecodeGetter)
        }
        publicSession!!.widgetInterceptor = widgetInterceptor
        return publicSession as LiveLikeContentSession
    }

    fun createPrivateSession(
        sessionId: String,
        widgetInterceptor: WidgetInterceptor? = null,
        errorDelegate: ErrorDelegate? = null
    ): LiveLikeContentSession {
        if (privateGroupChatsession == null || privateGroupChatsession?.contentSessionId() != sessionId) {
            privateGroupChatsession?.close()
            privateGroupChatsession = sdk.createContentSession(sessionId, timecodeGetter, errorDelegate)
        }
        privateGroupChatsession!!.widgetInterceptor = widgetInterceptor
        return privateGroupChatsession as LiveLikeContentSession
    }
}

const val PREFERENCES_APP_ID = BuildConfig.APP_CLIENT_ID + "Test_Demo"
