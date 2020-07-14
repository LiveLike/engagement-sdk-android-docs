package com.livelike.engagementsdk

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.jakewharton.threetenabp.AndroidThreeTen
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.ChatSession
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMemberListResponse
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembershipPagination
import com.livelike.engagementsdk.chat.data.remote.UserChatRoomListResponse
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.livelike.engagementsdk.core.EnagagementSdkUncaughtExceptionHandler
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.exceptionhelpers.BugsnagClient
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.combineLatestOnce
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getSharedAccessToken
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.setSharedAccessToken
import com.livelike.engagementsdk.core.utils.map
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeUserApi
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
    private val errorDelegate: ErrorDelegate? = null,
    private val originURL: String? = null,
    private var accessTokenDelegate: AccessTokenDelegate? = null
) : IEngagement {


    private var userChatRoomListResponse: UserChatRoomListResponse? = null
    private var chatRoomMemberListMap: MutableMap<String, ChatRoomMemberListResponse> =
        mutableMapOf()
    internal var configurationStream: Stream<SdkConfiguration> =
        SubscriptionManager()
    private val dataClient =
        EngagementDataClientImpl()

    internal val userRepository =
        UserRepository(clientId)

    private val job = SupervisorJob()

    // by default sdk calls will run on Default pool and further data layer calls will run o
    private val sdkScope = CoroutineScope(Dispatchers.Default + job)

    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    /**
     * SDK Initialization logic.
     */
    init {
        EnagagementSdkUncaughtExceptionHandler
        BugsnagClient.wouldInitializeBugsnagClient(applicationContext)
        AndroidThreeTen.init(applicationContext) // Initialize DateTime lib
        initLiveLikeSharedPrefs(
            applicationContext
        )
        if (accessTokenDelegate == null) {
            accessTokenDelegate = object : AccessTokenDelegate {
                override fun getAccessToken(): String? = getSharedAccessToken()

                override fun storeAccessToken(accessToken: String?) {
                    accessToken?.let { setSharedAccessToken(accessToken) }
                }
            }
        }
        userRepository.currentUserStream.subscribe(this.javaClass.simpleName) {
            it?.accessToken?.let { token ->
                userRepository.currentUserStream.unsubscribe(this.javaClass.simpleName)
                accessTokenDelegate!!.storeAccessToken(token)
            }
        }
        val url = originURL?.plus("/api/v1/applications/$clientId")
            ?: BuildConfig.CONFIG_URL.plus("applications/$clientId")
        dataClient.getEngagementSdkConfig(url) {
            if (it is Result.Success) {
                configurationStream.onNext(it.data)
                userRepository.initUser(accessTokenDelegate!!.getAccessToken(), it.data.profileUrl)
            } else {
                errorDelegate?.onError(
                    (it as Result.Error).exception.message
                        ?: "Some Error occurred, used sdk logger for more details"
                )
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

    override fun createChatRoom(title: String?, liveLikeCallback: LiveLikeCallback<ChatRoomInfo>) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) {
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin
                        )

                    uiScope.launch {
                        val chatRoomResult = chatRepository.createChatRoom(
                            title, pair.second.createChatRoomUrl
                        )
                        if (chatRoomResult is Result.Success) {
                            liveLikeCallback.onResponse(
                                ChatRoomInfo(
                                    chatRoomResult.data.id,
                                    chatRoomResult.data.title
                                ), null
                            )
                        } else if (chatRoomResult is Result.Error) {
                            liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                        }
                    }
                }
            }
    }

    override fun getChatRoom(id: String, liveLikeCallback: LiveLikeCallback<ChatRoomInfo>) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) {
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin
                        )

                    uiScope.launch {
                        val chatRoomResult = chatRepository.fetchChatRoom(
                            id, pair.second.chatRoomUrlTemplate
                        )
                        if (chatRoomResult is Result.Success) {
                            liveLikeCallback.onResponse(
                                ChatRoomInfo(
                                    chatRoomResult.data.id,
                                    chatRoomResult.data.title
                                ), null
                            )
                        } else if (chatRoomResult is Result.Error) {
                            liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                        }
                    }
                }
            }
    }

    override fun addCurrentUserToChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomMembership>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) {
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin
                        )
                    uiScope.launch {
                        val chatRoomResult = chatRepository.addCurrentUserToChatRoom(
                            chatRoomId, pair.second.createChatRoomUrl
                        )
                        if (chatRoomResult is Result.Success) {
                            liveLikeCallback.onResponse(
                                chatRoomResult.data, null
                            )
                        } else if (chatRoomResult is Result.Error) {
                            liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                        }
                    }
                }
            }
    }

    override fun getCurrentUserChatRoomList(
        chatRoomMembershipPagination: ChatRoomMembershipPagination,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInfo>>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) { it ->
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin
                        )
                    uiScope.launch {
                        val url = when (chatRoomMembershipPagination) {
                            ChatRoomMembershipPagination.FIRST -> pair.first.chat_room_memberships_url
                            ChatRoomMembershipPagination.NEXT -> userChatRoomListResponse?.next
                            ChatRoomMembershipPagination.PREVIOUS -> userChatRoomListResponse?.previous
                        }
                        val chatRoomResult = chatRepository.getCurrentUserChatRoomList(
                            url ?: pair.first.chat_room_memberships_url
                        )
                        if (chatRoomResult is Result.Success) {
                            userChatRoomListResponse = chatRoomResult.data
                            val list = userChatRoomListResponse!!.results?.map {
                                ChatRoomInfo(
                                    it?.chatRoom?.id!!,
                                    it.chatRoom.title
                                )
                            }
                            liveLikeCallback.onResponse(list, null)
                        } else if (chatRoomResult is Result.Error) {
                            liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                        }
                    }
                }
            }
    }

    override fun getMembersOfChatRoom(
        chatRoomId: String,
        chatRoomMembershipPagination: ChatRoomMembershipPagination,
        liveLikeCallback: LiveLikeCallback<List<LiveLikeUser>>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) { it ->
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin
                        )
                    uiScope.launch {
                        val url = when (chatRoomMembershipPagination) {
                            ChatRoomMembershipPagination.FIRST -> null
                            ChatRoomMembershipPagination.NEXT -> chatRoomMemberListMap[chatRoomId]?.next
                            ChatRoomMembershipPagination.PREVIOUS -> chatRoomMemberListMap[chatRoomId]?.previous
                        }
                        val chatRoomResult = chatRepository.getMembersOfChatRoom(
                            chatRoomId, pair.second.chatRoomUrlTemplate, paginationUrl = url
                        )
                        if (chatRoomResult is Result.Success) {
                            chatRoomMemberListMap[chatRoomId] = chatRoomResult.data
                            val list = chatRoomResult.data.results?.map {
                                it?.profile!!
                            }
                            liveLikeCallback.onResponse(list, null)
                        } else if (chatRoomResult is Result.Error) {
                            liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                        }
                    }
                }
            }
    }

    override fun deleteCurrentUserFromChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<Boolean>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) {
                it?.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin
                        )
                    uiScope.launch {
                        val chatRoomResult = chatRepository.deleteCurrentUserFromChatRoom(
                            chatRoomId, pair.second.chatRoomUrlTemplate
                        )
                        liveLikeCallback.onResponse(
                            true, null
                        )
                    }
                }
            }
    }

    /**
     *  Creates a content session without sync.
     *  @param programId Backend generated unique identifier for current program
     */
    fun createContentSession(
        programId: String,
        errorDelegate: ErrorDelegate? = null
    ): LiveLikeContentSession {
        return ContentSession(
            configurationStream,
            userRepository,
            applicationContext,
            programId,
            errorDelegate
        ) { EpochTime(0) }
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
    fun createContentSession(
        programId: String,
        timecodeGetter: TimecodeGetter,
        errorDelegate: ErrorDelegate? = null
    ): LiveLikeContentSession {
        return ContentSession(
            configurationStream,
            userRepository,
            applicationContext,
            programId,
            errorDelegate
        ) { timecodeGetter.getTimecode() }
    }

    /**
     *  Creates a chat session.
     *  @param programId Backend generated identifier for current program
     *  @param timecodeGetter returns the video timecode
     */
    fun createChatSession(
        timecodeGetter: TimecodeGetter,
        errorDelegate: ErrorDelegate? = null
    ): LiveLikeChatSession {
        return ChatSession(
            configurationStream,
            userRepository,
            applicationContext,
            false,
            errorDelegate
        ) { timecodeGetter.getTimecode() }
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
        @SerializedName("reaction_packs_url")
        val reactionPacksUrl: String,
        @SerializedName("mixpanel_token")
        val mixpanelToken: String,
        @SerializedName("analytics_properties")
        val analyticsProps: Map<String, String>,
        @SerializedName("chat_room_detail_url_template")
        val chatRoomUrlTemplate: String,
        @SerializedName("create_chat_room_url")
        val createChatRoomUrl: String,
        @SerializedName("profile_url")
        val profileUrl: String,
        @SerializedName("program_detail_url_template")
        val programDetailUrlTemplate: String,
        @SerializedName("pubnub_origin")
        val pubnubOrigin: String? = null
    )

    companion object {
        @JvmStatic
        var enableDebug: Boolean = false
    }

}
