package com.livelike.engagementsdk

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.jakewharton.threetenabp.AndroidThreeTen
import com.livelike.engagementsdk.core.EnagagementSdkUncaughtExceptionHandler
import com.livelike.engagementsdk.core.exceptionhelpers.BugsnagClient
import com.livelike.engagementsdk.data.repository.UserRepository
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeUserApi
import com.livelike.engagementsdk.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.services.network.Result
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs
import com.livelike.engagementsdk.utils.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Use this class to initialize the EngagementSDK. This is the entry point for SDK usage. This creates an instance of EngagementSDK.
 *
 * @param clientId Client's id
 * @param applicationContext The application context
 */
class EngagementSDK(
    private val clientId: String,
    private val applicationContext: Context,
    accessToken: String? = null,
    private val errorDelegate: ErrorDelegate? = null
) : IEngagement {

    companion object {
        @JvmStatic
        var enableDebug: Boolean = false
    }

    private var configurationStream: Stream<SdkConfiguration> = SubscriptionManager()
    private val dataClient = EngagementDataClientImpl()

    private val userRepository = UserRepository(clientId)

    private val job = SupervisorJob()
    // by default sdk calls will run on Default pool and further data layer calls will run o
    private val sdkScope = CoroutineScope(Dispatchers.Default + job)

    /**
     * SDK Initialization logic.
     */
    init {
        EnagagementSdkUncaughtExceptionHandler
        BugsnagClient.wouldInitializeBugsnagClient(applicationContext)
        AndroidThreeTen.init(applicationContext) // Initialize DateTime lib
        initLiveLikeSharedPrefs(applicationContext)
        dataClient.getEngagementSdkConfig(BuildConfig.CONFIG_URL.plus("applications/$clientId")) {
            if (it is Result.Success) {
                configurationStream.onNext(it.data)
                userRepository.initUser(accessToken, it.data.profileUrl)
            } else {
                errorDelegate?.onError((it as Result.Error).exception.message ?: "Some Error occurred, used sdk logger for more details")
            }
        }
    }

    override val userStream: Stream<LiveLikeUserApi>
        get() = userRepository.currentUserStream.map {
            LiveLikeUserApi(it.nickname, it.accessToken)
        }
    override val userAccessToken: String?
        get() = userRepository.userAccessToken

    override fun updateChatNickname(nickname: String) {
        sdkScope.launch {
            userRepository.updateChatNickname(nickname)
        }
    }

    override fun updateChatUserPic(url: String?) {
        sdkScope.launch {
            userRepository.setProfilePicUrl(url)
        }
    }

    /**
     *  Creates a content session without sync.
     *  @param programId Backend generated unique identifier for current program
     */
    fun createContentSession(programId: String, errorDelegate: ErrorDelegate? = null): LiveLikeContentSession {
        return ContentSession(
            configurationStream,
            userRepository,
            applicationContext,
            programId,
            errorDelegate) { EpochTime(0) }
    }

    /**
     * Use to retrieve the current timecode from the videoplayer to enable Spoiler-Free Sync.
     *
     */
    interface TimecodeGetter {
        fun getTimecode(): EpochTime
    }

    /**
     *  Creates a content session with sync.
     *  @param programId Backend generated identifier for current program
     *  @param timecodeGetter returns the video timecode
     */
    fun createContentSession(programId: String, timecodeGetter: TimecodeGetter, errorDelegate: ErrorDelegate? = null): LiveLikeContentSession {
        return ContentSession(
            configurationStream,
            userRepository,
            applicationContext,
            programId,
            errorDelegate) { timecodeGetter.getTimecode() }
    }

    internal data class SdkConfiguration(
        val url: String,
        val name: String?,
        @SerializedName("client_id")
        val clientId: String,
        @SerializedName("media_url")
        val mediaUrl: String,
        @SerializedName("pubnub_subscribe_key")
        val pubNubKey: String,
        @SerializedName("pubnub_publish_key")
        val pubnubPublishKey: String?,
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
        val analyticsProps: Map<String, String>,
        @SerializedName("chat_room_detail_url_template")
        val chatRoomUrlTemplate: String,
        @SerializedName("profile_url")
        val profileUrl: String,
        @SerializedName("program_detail_url_template")
        val programDetailUrlTemplate: String
    )
}
