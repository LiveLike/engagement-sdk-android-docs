package com.livelike.livelikesdk

import android.content.Context
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.network.LiveLikeDataClientImpl

/**
 * Use this class to initialize the LiveLike SDK. This is the entry point for SDK usage. This creates a singleton instance of LiveLike SDK.
 * The SDK is expected to be initialized only once. Once the SDK has been initialized, user can create multiple sessions
 * using [createContentSession]
 *
 * @param appId Application's id
 */

class LiveLikeSDK(val appId: String, val applicationContext: Context) {

    companion object {
        const val CONFIG_URL = "https://livelike-blast.herokuapp.com/api/v1/applications/"
    }

    lateinit var configuration: SdkConfiguration
    private val dataClient = LiveLikeDataClientImpl()
    init {

    }

    /**
     *  Creates a content session.
     *  @param contentId
     *  @param currentPlayheadTime
     */
    fun createContentSession(contentId: String,
                             currentPlayheadTime: () -> EpochTime,
                             sessionReady: (LiveLikeContentSession) -> Unit
    ) {
        sessionReady.invoke(createContentSession(contentId, currentPlayheadTime))
    }

    /**
     *  Creates a content session.
     *  @param contentId
     *  @param currentPlayheadTime
     */
    fun createContentSession(contentId: String, currentPlayheadTime: () -> EpochTime) : LiveLikeContentSession {
        return LiveLikeContentSessionImpl(contentId, currentPlayheadTime, object : Provider<SdkConfiguration> {
            override fun subscribe(ready: (SdkConfiguration) -> Unit) {
                dataClient.getLiveLikeSdkConfig(CONFIG_URL.plus(appId)) { ready(it) }
            }
        }, applicationContext )
    }

    data class SdkConfiguration(
        val url: String,
        val name: String,
        val clientId: String,
        val mediaUrl: String,
        val pubNubKey: String,
        val sendBirdAppId: String,
        val sendBirdEndpoint: String,
        val programsUrl: String,
        val sessionsUrl: String,
        val stickerPackUrl: String
    )
}

internal interface LiveLikeSdkDataClient {
    fun getLiveLikeSdkConfig(url: String, responseCallback: (config: LiveLikeSDK.SdkConfiguration) -> Unit)
}


