package com.livelike.livelikesdk

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.jakewharton.threetenabp.AndroidThreeTen
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.livelikesdk.data.repository.UserRepository
import com.livelike.livelikesdk.publicapis.IEngagement
import com.livelike.livelikesdk.publicapis.LiveLikeUserApi
import com.livelike.livelikesdk.services.messaging.proxies.WidgetInterceptor
import com.livelike.livelikesdk.services.network.EngagementDataClientImpl
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs
import com.livelike.livelikesdk.utils.map

/**
 * Use this class to initialize the EngagementSDK. This is the entry point for SDK usage. This creates an instance of EngagementSDK.
 *
 * @param clientId Client's id
 * @param applicationContext The application context
 */
class EngagementSDK(
    private val clientId: String,
    private val applicationContext: Context,
    private var accessToken: String? = null
) : IEngagement {

//    TODO : Handle if integrator intialize sdk in offline state ( Once I thought of creating stream of sdk initialization requests too and handle everything gracefully :) )

    private var configurationStream: Stream<SdkConfiguration> = SubscriptionManager()
    private val dataClient = EngagementDataClientImpl()

    private val userRepository = UserRepository

    private val currentUser: Stream<LiveLikeUser> = SubscriptionManager()

    override val userStream: Stream<LiveLikeUserApi>
        get() = userRepository.currentUserStream.map {
            LiveLikeUserApi(it.nickname, it.accessToken)
        }
    override val userAccessToken: String?
        get() = userRepository.userAccessToken

    init {
        AndroidThreeTen.init(applicationContext) // Initialize DateTime lib
        initLiveLikeSharedPrefs(applicationContext)
        dataClient.getEngagementSdkConfig(BuildConfig.CONFIG_URL.plus("applications/$clientId")) {
            configurationStream.onNext(it)
        }

        userRepository.initUser(clientId, userAccessToken)
    }

    /**
     *  Creates a content session without sync.
     *  @param programId Backend generated unique identifier for current program
     */
    fun createContentSession(programId: String, widgetInterceptor: WidgetInterceptor? = null): LiveLikeContentSession {
        return ContentSession(
            configurationStream,
            currentUser,
            applicationContext,
            programId,
            { EpochTime(0) },
            widgetInterceptor
        )
    }

    interface TimecodeGetter {
        fun getTimecode(): EpochTime
    }

    /**
     *  Creates a content session with sync.
     *  @param programId Backend generated identifier for current program
     *  @param timecodeGetter returns the video timecode
     */
    fun createContentSession(
        programId: String,
        timecodeGetter: TimecodeGetter,
        widgetInterceptor: WidgetInterceptor? = null
    ): LiveLikeContentSession {
        return ContentSession(
            configurationStream,
            currentUser,
            applicationContext,
            programId,
            { timecodeGetter.getTimecode() },
            widgetInterceptor
        )
    }

    data class SdkConfiguration(
        val url: String,
        val name: String?,
        @SerializedName("client_id")
        val clientId: String,
        @SerializedName("media_url")
        val mediaUrl: String,
        @SerializedName("pubnub_subscribe_key")
        val pubNubKey: String,
        @SerializedName("sendbird_app_id")
        val sendBirdAppId: String,
        @SerializedName("sendbird_api_endpoint")
        val sendBirdEndpoint: String,
        @SerializedName("programs_url")
        val programsUrl: String,
        @SerializedName("sessions_url")
        val sessionsUrl: String,
        @SerializedName("sticker_packs_url")
        val stickerPackUrl: String,
        @SerializedName("mixpanel_token")
        val mixpanelToken: String,
        @SerializedName("analytics_properties")
        val analyticsProps: Map<String, String>
    )
}
