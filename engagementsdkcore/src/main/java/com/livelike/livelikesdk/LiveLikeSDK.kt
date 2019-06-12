package com.livelike.livelikesdk

import android.content.Context
import com.jakewharton.threetenabp.AndroidThreeTen
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.services.analytics.analyticService
import com.livelike.livelikesdk.services.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs

private var sdkInstance: LiveLikeSDK? = null

/**
 * Use this class to initialize the LiveLike SDK. This is the entry point for SDK usage. This creates a singleton instance of LiveLike SDK.
 * The SDK is expected to be initialized only once. Once the SDK has been initialized, user can create multiple sessions
 * using [createContentSession]
 *
 * @param clientId Client's id
 * @param applicationContext The application context
 */
class LiveLikeSDK(val clientId: String, private val applicationContext: Context) {

    companion object {
        private const val CONFIG_URL = BuildConfig.CONFIG_URL
        val MIXPANEL_TOKEN = "5c82369365be76b28b3716f260fbd2f5" // TODO: This should come from CMS
        var configuration: SdkConfiguration? = null
    }

    private val dataClient = LiveLikeDataClientImpl()

    init {
        if (sdkInstance == null) {
            AndroidThreeTen.init(applicationContext) // Initialize DateTime lib
            initLiveLikeSharedPrefs(applicationContext)
            analyticService.initialize(applicationContext, MIXPANEL_TOKEN)
            sdkInstance = this
        }
    }

    /**
     *  Creates a content session without sync.
     *  @param programId Backend generated unique identifier for current program
     */
    fun createContentSession(programId: String): LiveLikeContentSession {
        val currentSession = LiveLikeContentSessionImpl(
            object : Provider<SdkConfiguration> {
                override fun subscribe(ready: (SdkConfiguration) -> Unit) {
                    if (configuration != null) ready(configuration!!)
                    else dataClient.getLiveLikeSdkConfig(CONFIG_URL.plus(clientId)) {
                        configuration = it
                        ready(it)
                    }
                }
            }, applicationContext
        )
        currentSession.programUrl = programId
        currentSession.currentPlayheadTime = { EpochTime(0) }
        return currentSession
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
        val currentSession = LiveLikeContentSessionImpl(
            object : Provider<SdkConfiguration> {
                override fun subscribe(ready: (SdkConfiguration) -> Unit) {
                    if (configuration != null) ready(configuration!!)
                    else dataClient.getLiveLikeSdkConfig(CONFIG_URL.plus(clientId)) {
                        configuration = it
                        ready(it)
                    }
                }
            }, applicationContext
        )
        currentSession.programUrl = programId
        currentSession.currentPlayheadTime = { timecodeGetter.getTimecode() }
        return currentSession
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
