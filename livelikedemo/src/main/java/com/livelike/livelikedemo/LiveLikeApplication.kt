package com.livelike.livelikedemo

import android.app.Application
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.exoplayer2.ui.PlayerView
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.core.utils.registerLogsHandler
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.video.ExoPlayerImpl
import com.livelike.livelikedemo.video.VideoPlayer

class LiveLikeApplication : Application() {

    lateinit var channelManager: ChannelManager
    var player: VideoPlayer? = null
    val timecodeGetter = object : EngagementSDK.TimecodeGetter {
        override fun getTimecode(): EpochTime {
            return EpochTime(player?.getPDT() ?: 0)
        }
    }
    var publicSession: LiveLikeContentSession? = null
    private var privateGroupChatsession: LiveLikeChatSession? = null


    lateinit var sdk: EngagementSDK
    lateinit var sdk2: EngagementSDK

    private var errorDelegate = object : ErrorDelegate() {
        override fun onError(error: String) {
            println("LiveLikeApplication.onError: $error")
        }
    }

    override fun onCreate() {
        super.onCreate()
//        TODO: THIS SHOULD BE FIXED ASAP
//        sdk2 = EngagementSDK("vjiRzT1wPpLEdgQwjWXN0TAuTx1KT7HljjDD4buA", applicationContext)
//        selectEnvironment("QA")
        reigsterLogsHandler()
    }

    private fun reigsterLogsHandler() {
        registerLogsHandler(object :
                (String) -> Unit {
            override fun invoke(text: String) {
                Log.d("engagement sdk logs : ", text)
            }
        })
    }

    fun selectEnvironment(key: String) {
        selectEnvironmentKey = key
        selectedEnvironment = environmentMap[key]
        initSDK()
    }

    fun initSDK() {
        selectedEnvironment?.let {
            channelManager = ChannelManager(it.testConfigUrl, applicationContext)
            sdk = EngagementSDK(
                it.clientId,
                applicationContext,
                object : ErrorDelegate() {
                    override fun onError(error: String) {
                        println("LiveLikeApplication.onError--->$error")
                        android.os.Handler(Looper.getMainLooper()).postDelayed(
                            {
                                initSDK()
                            },
                            1000
                        )
                    }
                },
                accessTokenDelegate = object : AccessTokenDelegate {
                    override fun getAccessToken(): String? {
                        return getSharedPreferences(
                            PREFERENCES_APP_ID,
                            Context.MODE_PRIVATE
                        ).getString(
                            PREF_USER_ACCESS_TOKEN,
                            null
                        ).apply {
                            println("Token:$this")
                        }
                    }

                    override fun storeAccessToken(accessToken: String?) {
                        getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).edit()
                            .putString(
                                PREF_USER_ACCESS_TOKEN, accessToken
                            ).apply()
                    }
                },
                originURL = it.configUrl
            )
        }
    }

    fun createPlayer(playerView: PlayerView): VideoPlayer {
        val playerTemp = ExoPlayerImpl(applicationContext, playerView)
        player = playerTemp
        return playerTemp
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
                sdk.createContentSession(sessionId, timecodeGetter, errorDelegate)
            else
                sdk.createContentSession(sessionId, errorDelegate)
        }
        publicSession!!.widgetInterceptor = widgetInterceptor
        return publicSession as LiveLikeContentSession
    }

    fun createPrivateSession(
        errorDelegate: ErrorDelegate? = null,
        timecodeGetter: EngagementSDK.TimecodeGetter? = null
    ): LiveLikeChatSession {
        if (privateGroupChatsession == null) {
            privateGroupChatsession?.close()
            privateGroupChatsession =
                sdk.createChatSession(timecodeGetter ?: this.timecodeGetter, errorDelegate)
        }
        return privateGroupChatsession as LiveLikeChatSession
    }

    fun createPrivateSessionForMultiple(
        errorDelegate: ErrorDelegate? = null,
        timecodeGetter: EngagementSDK.TimecodeGetter? = null
    ): LiveLikeChatSession {
        return sdk.createChatSession(timecodeGetter ?: this.timecodeGetter, errorDelegate)
    }

    companion object {
        var showCustomWidgetsUI: Boolean = false
         val environmentMap = hashMapOf(
            "Staging" to EnvironmentVariable(
                "vLgjH7dF0uX4J4FQJK3ncMkVmsCdLWhJ0qPtsbk7",
                "https://cf-blast-staging.livelikecdn.com",
                "https://cf-blast-staging.livelikecdn.com/api/v1/programs/?client_id=vLgjH7dF0uX4J4FQJK3ncMkVmsCdLWhJ0qPtsbk7"
            ),
            "Production" to EnvironmentVariable(
                "8PqSNDgIVHnXuJuGte1HdvOjOqhCFE1ZCR3qhqaS",
                "https://cf-blast.livelikecdn.com",
                "https://cf-blast.livelikecdn.com/api/v1/programs/?client_id=8PqSNDgIVHnXuJuGte1HdvOjOqhCFE1ZCR3qhqaS"
            ),
            "QA" to EnvironmentVariable(
                "pnODbVXg0UI80s0l2aH5Y7FOuGbftoAdSNqpdvo6",
                "https://cf-blast-qa.livelikecdn.com",
                "https://cf-blast-qa.livelikecdn.com/api/v1/programs/?client_id=pnODbVXg0UI80s0l2aH5Y7FOuGbftoAdSNqpdvo6"
            ),
            "QA Iconic" to EnvironmentVariable(
                "LT9lUmrzSqXvAL66rSWSGK0weclpFNbHANUTxW9O",
                "https://cf-blast-iconic.livelikecdn.com",
                "https://cf-blast-iconic.livelikecdn.com/api/v1/programs/?client_id=LT9lUmrzSqXvAL66rSWSGK0weclpFNbHANUTxW9O"
            ),
            "QA DIG" to EnvironmentVariable(
                "lom9db0XtQUhOZQq1vz8QPfSpiyyxppiUVGMcAje",
                "https://cf-blast-dig.livelikecdn.com",
                "https://cf-blast-dig.livelikecdn.com/api/v1/programs/?client_id=lom9db0XtQUhOZQq1vz8QPfSpiyyxppiUVGMcAje"
            ),
        )
        var selectEnvironmentKey = "Staging"
        var selectedEnvironment: EnvironmentVariable? = environmentMap["Staging"]
        val PREFERENCES_APP_ID
            get() = selectedEnvironment?.clientId + "Test_Demo"
        val CHAT_ROOM_LIST
            get() = selectedEnvironment?.clientId + "chat_rooms"

    }
}


data class EnvironmentVariable(
    val clientId: String,
    val configUrl: String,
    val testConfigUrl: String
)