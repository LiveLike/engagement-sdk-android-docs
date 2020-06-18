package com.livelike.livelikedemo

import android.app.Application
import android.content.Context
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.google.android.exoplayer2.ui.PlayerView
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.publicapis.ErrorDelegate
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
    var publicSession: LiveLikeContentSession? = null
    private var privateGroupChatsession: LiveLikeChatSession? = null

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
        sdk = EngagementSDK(
            BuildConfig.APP_CLIENT_ID,
            applicationContext,
            object : ErrorDelegate() {
                override fun onError(error: String) {
                    android.os.Handler(Looper.getMainLooper()).postDelayed({
                        initSDK()
                    }, 1000)
                }
            }, accessTokenDelegate = object : AccessTokenDelegate {
                override fun getAccessToken(): String? {
                    return getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).getString(
                        PREF_USER_ACCESS_TOKEN,
                        null
                    )
                }

                override fun storeAccessToken(accessToken: String?) {
                    getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).edit().putString(
                        PREF_USER_ACCESS_TOKEN, accessToken
                    ).apply()
                }

            })
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
        widgetInterceptor: WidgetInterceptor? = null,
        allowTimeCodeGetter: Boolean = true
    ): LiveLikeContentSession {
        if (publicSession == null || publicSession?.contentSessionId() != sessionId) {
            publicSession?.close()
            publicSession = if (allowTimeCodeGetter)
                sdk.createContentSession(sessionId, timecodeGetter)
            else
                sdk.createContentSession(sessionId)
        }
        publicSession!!.widgetInterceptor = widgetInterceptor
        return publicSession as LiveLikeContentSession
    }

    fun createPrivateSession(
        errorDelegate: ErrorDelegate? = null, timecodeGetter: EngagementSDK.TimecodeGetter? = null
    ): LiveLikeChatSession {
        if (privateGroupChatsession == null) {
            privateGroupChatsession?.close()
            privateGroupChatsession =
                sdk.createChatSession(timecodeGetter ?: this.timecodeGetter, errorDelegate)
        }
        return privateGroupChatsession as LiveLikeChatSession
    }
}

const val PREFERENCES_APP_ID = BuildConfig.APP_CLIENT_ID + "Test_Demo"
const val CHAT_ROOM_LIST = BuildConfig.APP_CLIENT_ID + "chat_rooms"
