package com.livelike.engagementsdk

import android.content.Context
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import com.jakewharton.threetenabp.AndroidThreeTen
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.ChatSession
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMemberListResponse
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.chat.data.remote.UserChatRoomListResponse
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.livelike.engagementsdk.core.EnagagementSdkUncaughtExceptionHandler
import com.livelike.engagementsdk.core.data.models.LeaderBoard
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntry
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntryResult
import com.livelike.engagementsdk.core.data.models.LeaderBoardResource
import com.livelike.engagementsdk.core.data.models.toLeadBoard
import com.livelike.engagementsdk.core.data.models.toReward
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.exceptionhelpers.BugsnagClient
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.combineLatestOnce
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getSharedAccessToken
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.setSharedAccessToken
import com.livelike.engagementsdk.core.utils.map
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeUserApi
import com.livelike.engagementsdk.widget.services.network.WidgetDataClientImpl
import java.io.IOException
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
        SubscriptionManager(true)
    private val dataClient =
        EngagementDataClientImpl()
    private val widgetDataClient = WidgetDataClientImpl()

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
        if (BuildConfig.DEBUG.not())
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

    override fun createChatRoom(
        title: String?,
        visibility: Visibility?,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    ) {
        createUpdateChatRoom(null, visibility, title, liveLikeCallback)
    }

    internal fun createUpdateChatRoom(
        chatRoomId: String?,
        visibility: Visibility?,
        title: String?,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
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
                        val chatRoomResult = when (chatRoomId == null) {
                            true -> chatRepository.createChatRoom(
                                title, visibility, pair.second.createChatRoomUrl
                            )
                            else -> chatRepository.updateChatRoom(
                                chatRoomId,
                                title, visibility, pair.second.createChatRoomUrl
                            )
                        }
                        if (chatRoomResult is Result.Success) {
                            liveLikeCallback.onResponse(
                                ChatRoomInfo(
                                    chatRoomResult.data.id,
                                    chatRoomResult.data.title,
                                    chatRoomResult.data.visibility
                                ), null
                            )
                        } else if (chatRoomResult is Result.Error) {
                            liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                        }
                    }
                }
            }
    }

    override fun updateChatRoom(
        chatRoomId: String,
        title: String?,
        visibility: Visibility?,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    ) {
        createUpdateChatRoom(chatRoomId, visibility, title, liveLikeCallback)
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
                                    chatRoomResult.data.title,
                                    chatRoomResult.data.visibility
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
        liveLikePagination: LiveLikePagination,
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
                        val url = when (liveLikePagination) {
                            LiveLikePagination.FIRST -> pair.first.chat_room_memberships_url
                            LiveLikePagination.NEXT -> userChatRoomListResponse?.next
                            LiveLikePagination.PREVIOUS -> userChatRoomListResponse?.previous
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
        liveLikePagination: LiveLikePagination,
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
                        val url = when (liveLikePagination) {
                            LiveLikePagination.FIRST -> null
                            LiveLikePagination.NEXT -> chatRoomMemberListMap[chatRoomId]?.next
                            LiveLikePagination.PREVIOUS -> chatRoomMemberListMap[chatRoomId]?.previous
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

    override fun getLeaderBoardsForProgram(
        programId: String,
        liveLikeCallback: LiveLikeCallback<List<LeaderBoard>>
    ) {
        configurationStream.subscribe(this) { configuration ->
            configuration?.let {
                configurationStream.unsubscribe(this)
                dataClient.getProgramData(
                    configuration.programDetailUrlTemplate.replace(
                        TEMPLATE_PROGRAM_ID,
                        programId
                    )
                ) { program ->
                    if (program?.leaderboards != null) {
                        liveLikeCallback.onResponse(program.leaderboards.map {
                            LeaderBoard(
                                it.id,
                                it.name,
                                it.rewardItem.toReward()
                            )
                        }, null)
                    } else {
                        liveLikeCallback.onResponse(null, "Unable to fetch LeaderBoards")
                    }
                }
            }
        }
    }

    override fun getLeaderBoardDetails(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoard>
    ) {
        configurationStream.subscribe(this) {
            it?.let {
                configurationStream.unsubscribe(this)
                uiScope.launch {
                    val url = "${it.leaderboardDetailUrlTemplate?.replace(
                        TEMPLATE_LEADER_BOARD_ID,
                        leaderBoardId
                    )}"
                    val result = dataClient.remoteCall<LeaderBoardResource>(
                        url,
                        requestType = RequestType.GET,
                        accessToken = null
                    )
                    if (result is Result.Success) {
                        liveLikeCallback.onResponse(
                            result.data.toLeadBoard(),
                            null
                        )
                    } else if (result is Result.Error) {
                        liveLikeCallback.onResponse(null, result.exception.message)
                    }
                }
            }
        }
    }

    private var leaderBoardEntryResult: HashMap<String, LeaderBoardEntryResult> = hashMapOf()

    override fun getEntriesForLeaderBoard(
        leaderBoardId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<LeaderBoardEntry>>
    ) {
        configurationStream.subscribe(this) {
            it?.let {
                configurationStream.unsubscribe(this)
                uiScope.launch {
                    val url = "${it.leaderboardDetailUrlTemplate?.replace(
                        TEMPLATE_LEADER_BOARD_ID,
                        leaderBoardId
                    )}"
                    val result = dataClient.remoteCall<LeaderBoardResource>(
                        url,
                        requestType = RequestType.GET,
                        accessToken = null
                    )
                    if (result is Result.Success) {
                        val defaultUrl = result.data.entries_url
                        val entriesUrl = when (liveLikePagination) {
                            LiveLikePagination.FIRST -> defaultUrl
                            LiveLikePagination.NEXT -> leaderBoardEntryResult[leaderBoardId]?.next
                            LiveLikePagination.PREVIOUS -> leaderBoardEntryResult[leaderBoardId]?.previous
                        }
                        if (entriesUrl != null) {
                            val listResult = dataClient.remoteCall<LeaderBoardEntryResult>(
                                entriesUrl,
                                requestType = RequestType.GET,
                                accessToken = null
                            )
                            if (listResult is Result.Success) {
                                leaderBoardEntryResult[leaderBoardId] = listResult.data
                                liveLikeCallback.onResponse(
                                    leaderBoardEntryResult[leaderBoardId]?.results,
                                    null
                                )
                            } else if (listResult is Result.Error) {
                                liveLikeCallback.onResponse(
                                    null,
                                    listResult.exception.message
                                )
                            }
                        } else {
                            liveLikeCallback.onResponse(null, "No More data to load")
                        }
                    } else if (result is Result.Error) {
                        liveLikeCallback.onResponse(null, result.exception.message)
                    }

                }
            }
        }

    }

    override fun getLeaderBoardEntryForProfile(
        leaderBoardId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    ) {
        configurationStream.subscribe(this) {
            it?.let {
                configurationStream.unsubscribe(this)
                uiScope.launch {
                    val url = "${it.leaderboardDetailUrlTemplate?.replace(
                        TEMPLATE_LEADER_BOARD_ID,
                        leaderBoardId
                    )}"
                    val result = dataClient.remoteCall<LeaderBoardResource>(
                        url,
                        requestType = RequestType.GET,
                        accessToken = null
                    )
                    if (result is Result.Success) {
                        val profileResult = dataClient.remoteCall<LeaderBoardEntry>(
                            result.data.entry_detail_url_template.replace(TEMPLATE_PROFILE_ID, profileId),
                            requestType = RequestType.GET,
                            accessToken = null
                        )
                        if (profileResult is Result.Success) {
                            liveLikeCallback.onResponse(profileResult.data, null)
                        } else if (profileResult is Result.Error) {
                            liveLikeCallback.onResponse(null, profileResult.exception.message)
                        }
                    } else if (result is Result.Error) {
                        liveLikeCallback.onResponse(null, result.exception.message)
                    }

                }
            }
        }


        getLeaderBoardDetails(leaderBoardId, object : LiveLikeCallback<LeaderBoard>() {
            override fun onResponse(result: LeaderBoard?, error: String?) {
                result?.let {
                    uiScope.launch {

                    }
                }
                error?.let {
                    liveLikeCallback.onResponse(null, error)
                }
            }
        })
    }

    override fun getLeaderBoardEntryForCurrentUserProfile(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    ) {
        userRepository.currentUserStream.subscribe(this) {
            it?.let { user ->
                userRepository.currentUserStream.unsubscribe(this)
                getLeaderBoardEntryForProfile(leaderBoardId, user.id, liveLikeCallback)
            }
        }
    }

    fun fetchWidgetDetails(
        widgetId: String,
        widgetKind: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeWidget>
    ) {
        uiScope.launch {
            try {
                val jsonObject = widgetDataClient.getWidgetDataFromIdAndKind(widgetId, widgetKind)
                liveLikeCallback.onResponse(
                    gson.fromJson(jsonObject, LiveLikeWidget::class.java),
                    null
                )
            } catch (e: JsonParseException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            } catch (e: IOException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
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
            clientId,
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
            clientId,
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
            clientId,
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
        val pubnubOrigin: String? = null,
        @SerializedName("leaderboard_detail_url_template")
        val leaderboardDetailUrlTemplate: String? = null
    )

    companion object {
        @JvmStatic
        var enableDebug: Boolean = false
    }
}
