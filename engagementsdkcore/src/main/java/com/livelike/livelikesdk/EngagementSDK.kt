package com.livelike.livelikesdk

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.jakewharton.threetenabp.AndroidThreeTen
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.livelikesdk.coreapis.IEngagement
import com.livelike.livelikesdk.services.network.EngagementDataClientImpl
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getNickename
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs

/**
 * Use this class to initialize the EngagementSDK. This is the entry point for SDK usage. This creates an instance of EngagementSDK.
 *
 * @param clientId Client's id
 * @param applicationContext The application context
 */
class EngagementSDK(
    private val clientId: String,
    private val userAccessToken: String? = null,
    private val applicationContext: Context
) : IEngagement {

//    TODO : Handle if integrator intialize sdk in offline state ( Once I thought of creating stream of sdk initialization requests too and handle everything gracefully :) )

    private var configurationStream: Stream<SdkConfiguration> = SubscriptionManager()
    private val dataClient = EngagementDataClientImpl()
    private val currentUser: Stream<LiveLikeUser> = SubscriptionManager()

    init {
        AndroidThreeTen.init(applicationContext) // Initialize DateTime lib
        initLiveLikeSharedPrefs(applicationContext)
        dataClient.getEngagementSdkConfig(BuildConfig.CONFIG_URL.plus("applications/$clientId")) {
            configurationStream.onNext(it)
        }

        initUser(userAccessToken)
    }

    /**
     *  getUser associated with the current sdk initialization
     *  user returned will be new if no access-token passed during sdk initialization
     *  null value means sdk initialization process not completed
     */
    private fun getUser(): LiveLikeUser? {
        return currentUser.latest()
    }

    override fun initUser(userAccessToken: String?) {

        if (userAccessToken == null) {
            dataClient.createUserData(clientId) {
                publishUser(it)
            }
        } else {
            dataClient.getUserData(clientId, accessToken = userAccessToken) {
                // TODO add Result class over evert network result instead of treating null as a case of invalid access token
                if (it == null) {
                    initUser(null)
                } else {
                    publishUser(it)
                }
            }
        }
    }

    private fun publishUser(it: LiveLikeUser) {
        val nickname =
            getNickename() // Checking again the saved nickname as it could have changed during the web request.
        if (nickname.isNotEmpty()) {
            it.nickname = nickname
        }
        currentUser.onNext(it)
    }

    override fun getUserAccessToken(): String? {
        return getUser()?.accessToken
    }

    /**
     *  Creates a content session without sync.
     *  @param programId Backend generated unique identifier for current program
     */
    fun createContentSession(programId: String): LiveLikeContentSession {
        return ContentSession(
            configurationStream,
            currentUser,
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
            configurationStream,
            currentUser,
            applicationContext,
            programId
        ) { timecodeGetter.getTimecode() }
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
