package com.livelike.livelikesdk

import android.content.Context
import com.jakewharton.threetenabp.AndroidThreeTen
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.services.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.utils.Provider
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs

/**
 * Use this class to initialize the LiveLike SDK. This is the entry point for SDK usage. This creates a singleton instance of LiveLike SDK.
 * The SDK is expected to be initialized only once. Once the SDK has been initialized, user can create multiple sessions
 * using [createContentSession]
 *
 * @param clientId Client's id
 * @param applicationContext The application context
 */
class LiveLikeSDK(val clientId: String, private val applicationContext: Context) {

    private val CONFIG_URL = BuildConfig.CONFIG_URL
    var configuration: SdkConfiguration? = null
    var configurationProvider: Provider<SdkConfiguration> = object : Provider<SdkConfiguration> {
        override fun subscribe(ready: (SdkConfiguration) -> Unit) {
            if (configuration != null) ready(configuration!!)
            else dataClient.getLiveLikeSdkConfig(CONFIG_URL.plus(clientId)) {
                configuration = it
                ready(it)
            }
        }
    }
    private val dataClient = LiveLikeDataClientImpl()

    init {
        AndroidThreeTen.init(applicationContext) // Initialize DateTime lib
        initLiveLikeSharedPrefs(applicationContext)
    }

    /**
     *  Creates a content session without sync.
     *  @param programId Backend generated unique identifier for current program
     */
    fun createContentSession(programId: String): LiveLikeContentSession {
        return LiveLikeContentSessionImpl(
            configurationProvider,
            applicationContext,
            programId
        ) { EpochTime(0) }
    }

    interface TimecodeGetter {
        fun getTimecode(): EpochTime
    }

    /**
     *  Creates a content session with sync.
     *  @param programId Backend generated identifier for current program
     *  @param timecodeGetter returns the video timecode
     */
    fun createContentSession(programId: String, timecodeGetter: TimecodeGetter): LiveLikeContentSession {
        return LiveLikeContentSessionImpl(
            configurationProvider,
            applicationContext,
            programId
        ) { timecodeGetter.getTimecode() }
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
        val stickerPackUrl: String,
        val mixpanelToken: String
    )
}

internal interface LiveLikeSdkDataClient {
    fun getLiveLikeSdkConfig(url: String, responseCallback: (config: LiveLikeSDK.SdkConfiguration) -> Unit)
}
