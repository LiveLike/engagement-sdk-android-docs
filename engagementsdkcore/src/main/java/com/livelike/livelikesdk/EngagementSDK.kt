package com.livelike.livelikesdk

import android.content.Context
import com.jakewharton.threetenabp.AndroidThreeTen
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.services.network.EngagementDataClientImpl
import com.livelike.livelikesdk.utils.Provider
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs

/**
 * Use this class to initialize the EngagementSDK. This is the entry point for SDK usage. This creates an instance of EngagementSDK.
 *
 * @param clientId Client's id
 * @param applicationContext The application context
 */
class EngagementSDK(val clientId: String, private val applicationContext: Context) {

    private val CONFIG_URL = BuildConfig.CONFIG_URL
    var configuration: SdkConfiguration? = null
    private var configurationProvider: Provider<SdkConfiguration> = object : Provider<SdkConfiguration> {
        override fun subscribe(ready: (SdkConfiguration) -> Unit) {
            if (configuration != null) ready(configuration!!)
            else dataClient.getEngagementSdkConfig(CONFIG_URL.plus("applications/$clientId")) {
                configuration = it
                ready(it)
            }
        }
    }
    private val dataClient = EngagementDataClientImpl()

    init {
        AndroidThreeTen.init(applicationContext) // Initialize DateTime lib
        initLiveLikeSharedPrefs(applicationContext)
    }

    /**
     *  Creates a content session without sync.
     *  @param programId Backend generated unique identifier for current program
     */
    fun createContentSession(programId: String): LiveLikeContentSession {
        return ContentSession(
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
        return ContentSession(
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
