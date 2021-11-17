package com.livelike.engagementsdk

import android.content.Context
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import com.jakewharton.threetenabp.AndroidThreeTen
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.ChatSession
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.chat.data.remote.*
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.chat.data.repository.ChatRoomRepository
import com.livelike.engagementsdk.chat.services.messaging.pubnub.PubnubChatRoomMessagingClient
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.livelike.engagementsdk.core.data.models.*
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.*
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getSharedAccessToken
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.setSharedAccessToken
import com.livelike.engagementsdk.gamification.Badges
import com.livelike.engagementsdk.gamification.IRewardsClient
import com.livelike.engagementsdk.gamification.Rewards
import com.livelike.engagementsdk.publicapis.*
import com.livelike.engagementsdk.sponsorship.Sponsor
import com.livelike.engagementsdk.gamification.InternalLiveLikeLeaderBoardClient
import com.livelike.engagementsdk.gamification.LiveLikeLeaderBoardClient
import com.livelike.engagementsdk.widget.data.respository.LocalPredictionWidgetVoteRepository
import com.livelike.engagementsdk.widget.data.respository.PredictionWidgetVoteRepository
import com.livelike.engagementsdk.widget.domain.LeaderBoardDelegate
import com.livelike.engagementsdk.widget.domain.UserProfileDelegate
import com.livelike.engagementsdk.widget.services.network.WidgetDataClientImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

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
    private var userSearchApiResponse: UserSearchApiResponse? = null
    internal var configurationStream: Stream<SdkConfiguration> =
        SubscriptionManager(true)
    private val dataClient =
        EngagementDataClientImpl()
    private val widgetDataClient = WidgetDataClientImpl()

    internal val userRepository =
        UserRepository(clientId)

    override var userProfileDelegate: UserProfileDelegate? = null
        set(value) {
            field = value
            userRepository.userProfileDelegate = value
        }

    override var leaderBoardDelegate: LeaderBoardDelegate? = null
        set(value) {
            field = value
            userRepository.leaderBoardDelegate = value
        }
    override var chatRoomDelegate: ChatRoomDelegate? = null
        set(value) {
            field = value
            setUpPubNubClient()
        }

    override var analyticService: Stream<AnalyticsService> =
        SubscriptionManager()

    private val job = SupervisorJob()

    // by default sdk calls will run on Default pool and further data layer calls will run o
    internal val sdkScope = CoroutineScope(Dispatchers.Default + job)

    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    // sdk config-user flow that can be collected by collect which is suspendably instead of using rx style combine on 2 seperate async results
    // TODO add util fun to convert streams to flow
    internal val configurationUserPairFlow = flow {
        while (configurationStream.latest() == null || userRepository.currentUserStream.latest() == null) {
            delay(1000)
        }
        emit(Pair(userRepository.currentUserStream.latest()!!, configurationStream.latest()!!))
    }

    /**
     * SDK Initialization logic.
     */
    init {
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
        userRepository.currentUserStream.subscribe(this.javaClass.simpleName) { user ->
            user?.accessToken?.let { token ->
                userRepository.currentUserStream.unsubscribe(this.javaClass.simpleName)
                accessTokenDelegate!!.storeAccessToken(token)
            }
            setUpPubNubClient()
        }
        val url = originURL?.plus("/api/v1/applications/$clientId")
            ?: BuildConfig.CONFIG_URL.plus("applications/$clientId")
        dataClient.getEngagementSdkConfig(url) {
            if (it is Result.Success) {
                configurationStream.onNext(it.data)
                it.data.mixpanelToken?.let { token ->
                    analyticService.onNext(
                        MixpanelAnalytics(
                            applicationContext,
                            token,
                            it.data.clientId
                        )
                    )
                }
                userRepository.initUser(accessTokenDelegate!!.getAccessToken(), it.data.profileUrl)
            } else {
                errorDelegate?.onError(
                    (it as Result.Error).exception.message
                        ?: "Some Error occurred, used sdk logger for more details"
                )
            }
        }
    }

    private var pubnubClient: PubnubChatRoomMessagingClient? = null

    private fun setUpPubNubClient() {
        if (pubnubClient == null && chatRoomDelegate != null) {
            configurationStream.latest()?.let { config ->
                userRepository.currentUserStream.latest()?.let {
                    pubnubClient = PubnubChatRoomMessagingClient(
                        config.pubNubKey,
                        config.pubnubHeartbeatInterval,
                        it.id,
                        config.pubnubPresenceTimeout
                    )
                    pubnubClient?.chatRoomDelegate = chatRoomDelegate
                    pubnubClient?.subscribe(arrayListOf(it.subscribeChannel!!))
                }
            }
        }
    }

    override val userStream: Stream<LiveLikeUserApi>
        get() = userRepository.currentUserStream.map {
            LiveLikeUserApi(it.nickname, it.accessToken, it.id, it.custom_data)
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

    private fun createUpdateChatRoom(
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
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )

                    uiScope.launch {
                        val chatRoomResult = when (chatRoomId == null) {
                            true -> chatRepository.createChatRoom(
                                title, visibility, pair.second.createChatRoomUrl
                            )
                            else -> chatRepository.updateChatRoom(
                                title, visibility, chatRoomId, pair.second.chatRoomDetailUrlTemplate
                            )
                        }
                        if (chatRoomResult is Result.Success) {
                            liveLikeCallback.onResponse(
                                ChatRoomInfo(
                                    chatRoomResult.data.id,
                                    chatRoomResult.data.title,
                                    chatRoomResult.data.visibility,
                                    chatRoomResult.data.contentFilter,
                                    chatRoomResult.data.customData
                                ),
                                null
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
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )

                    uiScope.launch {
                        val chatRoomResult = chatRepository.fetchChatRoom(
                            id, pair.second.chatRoomDetailUrlTemplate
                        )
                        if (chatRoomResult is Result.Success) {
                            liveLikeCallback.onResponse(
                                ChatRoomInfo(
                                    chatRoomResult.data.id,
                                    chatRoomResult.data.title,
                                    chatRoomResult.data.visibility,
                                    chatRoomResult.data.contentFilter,
                                    chatRoomResult.data.customData
                                ),
                                null
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
        addUserToChatRoom(chatRoomId, "", liveLikeCallback)
    }

    override fun addUserToChatRoom(
        chatRoomId: String,
        userId: String,
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
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )
                    uiScope.launch {
                        val chatRoomResult = chatRepository.fetchChatRoom(
                            chatRoomId, pair.second.chatRoomDetailUrlTemplate
                        )
                        if (chatRoomResult is Result.Success) {
                            val currentUserChatRoomResult =
                                dataClient.remoteCall<ChatRoomMembership>(
                                    chatRoomResult.data.membershipsUrl,
                                    accessToken = pair.first.accessToken,
                                    requestType = RequestType.POST,
                                    fullErrorJson = true,
                                    requestBody = when (userId.isEmpty()) {
                                        true -> byteArrayOf().toRequestBody(null, 0, 0)
                                        else -> """{"profile_id":"$userId"}"""
                                            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                    }
                                )
                            if (currentUserChatRoomResult is Result.Success) {
                                liveLikeCallback.onResponse(
                                    currentUserChatRoomResult.data, null
                                )
                            } else if (currentUserChatRoomResult is Result.Error) {
                                liveLikeCallback.onResponse(
                                    null,
                                    currentUserChatRoomResult.exception.message
                                )
                            }
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
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )
                    uiScope.launch {
                        val url = when (liveLikePagination) {
                            LiveLikePagination.FIRST -> pair.first.chat_room_memberships_url
                            LiveLikePagination.NEXT -> userChatRoomListResponse?.next
                            LiveLikePagination.PREVIOUS -> userChatRoomListResponse?.previous
                        }
                        if (url != null) {
                            val chatRoomResult = chatRepository.getCurrentUserChatRoomList(
                                url
                            )
                            if (chatRoomResult is Result.Success) {
                                userChatRoomListResponse = chatRoomResult.data
                                val list = userChatRoomListResponse!!.results?.map {
                                    ChatRoomInfo(
                                        it?.chatRoom?.id!!,
                                        it.chatRoom.title,
                                        it.chatRoom.visibility,
                                        it.chatRoom.contentFilter,
                                        it.chatRoom.customData
                                    )
                                }
                                liveLikeCallback.onResponse(list, null)
                            } else if (chatRoomResult is Result.Error) {
                                liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                            }
                        } else {
                            liveLikeCallback.onResponse(null, "No More data to load")
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
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )

                    uiScope.launch {
                        val chatRoomResult = chatRepository.fetchChatRoom(
                            chatRoomId, pair.second.chatRoomDetailUrlTemplate
                        )
                        if (chatRoomResult is Result.Success) {
                            val url = when (liveLikePagination) {
                                LiveLikePagination.FIRST -> chatRoomResult.data.membershipsUrl
                                LiveLikePagination.NEXT -> chatRoomMemberListMap[chatRoomId]?.next
                                LiveLikePagination.PREVIOUS -> chatRoomMemberListMap[chatRoomId]?.previous
                            }
                            if (url != null) {
                                val chatRoomMemberResult =
                                    dataClient.remoteCall<ChatRoomMemberListResponse>(
                                        url,
                                        accessToken = pair.first.accessToken,
                                        requestType = RequestType.GET
                                    )
                                if (chatRoomMemberResult is Result.Success) {
                                    chatRoomMemberListMap[chatRoomId] = chatRoomMemberResult.data
                                    val list = chatRoomMemberResult.data.results?.map {
                                        it?.profile!!
                                    }
                                    liveLikeCallback.onResponse(list, null)
                                } else if (chatRoomMemberResult is Result.Error) {
                                    liveLikeCallback.onResponse(
                                        null,
                                        chatRoomMemberResult.exception.message
                                    )
                                }
                            } else {
                                liveLikeCallback.onResponse(null, "No More data to load")
                            }
                        } else if (chatRoomResult is Result.Error) {
                            liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                        }
                    }
                }
            }
    }

    override fun deleteCurrentUserFromChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
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
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )
                    uiScope.launch {
                       val chatRoomResult= chatRepository.deleteCurrentUserFromChatRoom(
                            chatRoomId, pair.second.chatRoomDetailUrlTemplate
                        )
                        liveLikeCallback.processResult(chatRoomResult)
                    }
                }
            }
    }

    override fun getLeaderBoardsForProgram(
        programId: String,
        liveLikeCallback: LiveLikeCallback<List<LeaderBoard>>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getLeaderBoardsForProgram(
            programId,
            liveLikeCallback
        )
    }

    override fun getLeaderBoardDetails(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoard>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getLeaderBoardDetails(
            leaderBoardId,
            liveLikeCallback
        )
    }

    override fun getLeaderboardClients(
        leaderBoardId: List<String>,
        liveLikeCallback: LiveLikeCallback<LeaderboardClient>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getLeaderboardClients(
            leaderBoardId,
            liveLikeCallback
        )
    }

    override fun getChatUserMutedStatus(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatUserMuteStatus>
    ) {
        sdkScope.launch {
            getChatRoom(chatRoomId).collect {
                if (it is Result.Success) {
                    configurationUserPairFlow.collect { pair ->
                        val url = it.data.mutedStatusUrlTemplate ?: ""
                        liveLikeCallback.processResult(
                            ChatRoomRepository.getUserRoomMuteStatus(
                                url.replace(TEMPLATE_PROFILE_ID, pair.first.id)
                            )
                        )
                    }
                } else if (it is Result.Error) {
                    liveLikeCallback.processResult(it)
                }
            }
        }
    }

    override fun getCurrentUserDetails(liveLikeCallback: LiveLikeCallback<LiveLikeUserApi>) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) { pair ->
                pair?.let { _ ->
                    dataClient.getUserData(pair.second.profileUrl, pair.first.accessToken) {
                        if (it == null) {
                            liveLikeCallback.onResponse(
                                null,
                                "Network error or invalid access token"
                            )
                        } else {
                            liveLikeCallback.onResponse(
                                LiveLikeUserApi(
                                    it.nickname,
                                    it.accessToken,
                                    it.id,
                                    it.custom_data
                                ),
                                null
                            )
                        }
                    }
                }
            }
    }

    override fun sendChatRoomInviteToUser(
        chatRoomId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomInvitation>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) {
                it?.let {
                    uiScope.launch {
                        val result = dataClient.remoteCall<ChatRoomInvitation>(
                            it.second.createChatRoomInvitationUrl,
                            RequestType.POST,
                            requestBody = """{"chat_room_id":"$chatRoomId","invited_profile_id":"$profileId"}"""
                                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
                            userAccessToken, true
                        )
                        liveLikeCallback.processResult(result)
                    }
                }
            }
    }

    override fun updateChatRoomInviteStatus(
        chatRoomInvitation: ChatRoomInvitation,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<ChatRoomInvitation>
    ) {
        uiScope.launch {
            val result = dataClient.remoteCall<ChatRoomInvitation>(
                chatRoomInvitation.url,
                RequestType.PATCH,
                requestBody = """{"status":"${invitationStatus.key}"}"""
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
                userAccessToken,
                true
            )
            liveLikeCallback.processResult(result)
        }
    }

    override fun getInvitationsForCurrentProfileWithInvitationStatus(
        liveLikePagination: LiveLikePagination,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInvitation>>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) {
                it?.let {
                    uiScope.launch {
                        val url = when (liveLikePagination) {
                            LiveLikePagination.FIRST -> it.second.profileChatRoomReceivedInvitationsUrlTemplate.replace(
                                "{profile_id}",
                                it.first.id
                            ).replace("{status}", invitationStatus.key)
                            LiveLikePagination.NEXT -> invitationForProfileMap[it.first.id]?.next
                            LiveLikePagination.PREVIOUS -> invitationForProfileMap[it.first.id]?.previous
                        }
                        if (url != null) {
                            val result = dataClient.remoteCall<ChatRoomInvitationResponse>(
                                url,
                                RequestType.GET,
                                requestBody = null,
                                userAccessToken, true
                            )
                            if (result is Result.Success) {
                                invitationForProfileMap[it.first.id] = result.data
                                liveLikeCallback.onResponse(result.data.results, null)
                            } else if (result is Result.Error) {
                                liveLikeCallback.onResponse(
                                    null,
                                    result.exception.message
                                )
                            }
                        } else {
                            liveLikeCallback.onResponse(null, "No More data to load")
                        }
                    }
                }
            }
    }

    override fun getInvitationsByCurrentProfileWithInvitationStatus(
        liveLikePagination: LiveLikePagination,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInvitation>>
    ) {
        userRepository.currentUserStream.combineLatestOnce(configurationStream, this.hashCode())
            .subscribe(this) {
                it?.let {
                    uiScope.launch {
                        val url = when (liveLikePagination) {
                            LiveLikePagination.FIRST -> it.second.profileChatRoomSentInvitationsUrlTemplate.replace(
                                "{invited_by_id}",
                                it.first.id
                            ).replace("{status}", invitationStatus.key)
                            LiveLikePagination.NEXT -> invitationByProfileMap[it.first.id]?.next
                            LiveLikePagination.PREVIOUS -> invitationByProfileMap[it.first.id]?.previous
                        }
                        if (url != null) {
                            val result = dataClient.remoteCall<ChatRoomInvitationResponse>(
                                url,
                                RequestType.GET,
                                requestBody = null,
                                userAccessToken, true
                            )
                            if (result is Result.Success) {
                                invitationByProfileMap[it.first.id] = result.data
                                liveLikeCallback.onResponse(result.data.results, null)
                            } else if (result is Result.Error) {
                                liveLikeCallback.onResponse(
                                    null,
                                    result.exception.message
                                )
                            }
                        } else {
                            liveLikeCallback.onResponse(null, "No More data to load")
                        }
                    }
                }
            }
    }

    override fun blockProfile(
        profileId: String,
        liveLikeCallback: LiveLikeCallback<BlockedData>
    ) {
        userRepository.currentUserStream.subscribe(this) {
            it?.blockProfileUrl?.let { url ->
                uiScope.launch {
                    val result = dataClient.remoteCall<BlockedData>(
                        url,
                        RequestType.POST,
                        requestBody = """{"blocked_profile_id":"$profileId"}"""
                            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
                        userAccessToken,
                        true
                    )
                    liveLikeCallback.processResult(result)
                }
            }
        }
    }

    override fun unBlockProfile(
        blockId: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
    ) {
        userRepository.currentUserStream.subscribe(this) {
            it?.blockProfileUrl?.let { url ->
                uiScope.launch {
                    val result = dataClient.remoteCall<LiveLikeEmptyResponse>(
                        "$url$blockId/",
                        RequestType.DELETE,
                        requestBody = null,
                        userAccessToken,
                        true
                    )
                    liveLikeCallback.processResult(result)
                }
            }
        }
    }

    override fun getBlockedProfileList(
        liveLikePagination: LiveLikePagination,
        blockedProfileId: String?,
        liveLikeCallback: LiveLikeCallback<List<BlockedData>>
    ) {
        userRepository.currentUserStream.subscribe(this) {
            it?.blockProfileListTemplate?.let {
                uiScope.launch {
                    val params = when (blockedProfileId != null) {
                        true -> "blocked_profile_id=$blockedProfileId"
                        else -> ""
                    }
                    val url = when (liveLikePagination) {
                        LiveLikePagination.FIRST -> it.replace(
                            "{blocked_profile_id}",
                            blockedProfileId ?: ""
                        )
                        LiveLikePagination.NEXT -> blockedProfileListResponseMap[params]?.next
                        LiveLikePagination.PREVIOUS -> blockedProfileListResponseMap[params]?.previous
                    }
                    if (url != null) {
                        val result = dataClient.remoteCall<BlockedProfileListResponse>(
                            url,
                            RequestType.GET,
                            requestBody = null,
                            userAccessToken, true
                        )

                        if (result is Result.Success) {
                            blockedProfileListResponseMap[params] = result.data
                            liveLikeCallback.onResponse(result.data.results, null)
                        } else if (result is Result.Error) {
                            liveLikeCallback.onResponse(
                                null,
                                result.exception.message
                            )
                        }
                    } else {
                        liveLikeCallback.onResponse(null, "No More data to load")
                    }
                }
            }
        }
    }


    private val invitationForProfileMap = hashMapOf<String, ChatRoomInvitationResponse>()
    private val invitationByProfileMap = hashMapOf<String, ChatRoomInvitationResponse>()
    private var blockedProfileListResponseMap = hashMapOf<String, BlockedProfileListResponse>()


    override fun sponsor(): Sponsor {
        return Sponsor(this)
    }

    override fun badges(): Badges {
        return Badges(configurationStream, dataClient, sdkScope)
    }

    override fun rewards(): IRewardsClient {
        return Rewards(configurationUserPairFlow, dataClient, sdkScope)
    }

    /**
     * Closing all the services , stream and clear the variable
     * TODO: all stream close,instance clear
     */
    override fun close() {
        pubnubClient?.destroy()
        analyticService.latest()?.destroy()
        analyticService.clear()
    }

    internal suspend fun getChatRoom(chatRoomId: String): Flow<Result<ChatRoom>> {
        return flow {
            configurationUserPairFlow.collect {
                it.let { pair ->
                    val chatRepository =
                        ChatRepository(
                            pair.second.pubNubKey,
                            pair.first.accessToken,
                            pair.first.id,
                            MockAnalyticsService(),
                            pair.second.pubnubPublishKey,
                            origin = pair.second.pubnubOrigin,
                            pubnubHeartbeatInterval = pair.second.pubnubHeartbeatInterval,
                            pubnubPresenceTimeout = pair.second.pubnubPresenceTimeout
                        )

                    val chatRoomResult = chatRepository.fetchChatRoom(
                        chatRoomId, pair.second.chatRoomDetailUrlTemplate
                    )
                    emit(chatRoomResult)
                }
            }
        }
    }

    private val internalLeaderBoardClient =
        InternalLiveLikeLeaderBoardClient(configurationStream, userRepository, uiScope, dataClient)

    override fun leaderboard(): LiveLikeLeaderBoardClient = internalLeaderBoardClient


    override fun getEntriesForLeaderBoard(
        leaderBoardId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntryPaginationResult>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getEntriesForLeaderBoard(
            leaderBoardId,
            liveLikePagination,
            liveLikeCallback
        )
    }

    override fun getLeaderBoardEntryForProfile(
        leaderBoardId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getLeaderBoardEntryForProfile(
            leaderBoardId,
            profileId,
            liveLikeCallback
        )
    }

    override fun getLeaderBoardEntryForCurrentUserProfile(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getLeaderBoardEntryForCurrentUserProfile(
            leaderBoardId,
            liveLikeCallback
        )

    }

    fun fetchWidgetDetails(
        widgetId: String,
        widgetKind: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeWidget>
    ) {
        uiScope.launch {
            try {
                val jsonObject = widgetDataClient.getWidgetDataFromIdAndKind(widgetId, widgetKind)
                val widget = gson.fromJson(jsonObject, LiveLikeWidget::class.java)
                liveLikeCallback.onResponse(
                    widget,
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
            analyticService,
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
            analyticService,
            errorDelegate
        ) { timecodeGetter.getTimecode() }.apply {
            this.engagementSDK = this@EngagementSDK
        }
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
            analyticService,
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
        val chatRoomDetailUrlTemplate: String,
        @SerializedName("create_chat_room_url")
        val createChatRoomUrl: String,
        @SerializedName("profile_url")
        val profileUrl: String,
        @SerializedName("profile_detail_url_template")
        val profileDetailUrlTemplate: String,
        @SerializedName("program_detail_url_template")
        val programDetailUrlTemplate: String,
        @SerializedName("pubnub_origin")
        val pubnubOrigin: String? = null,
        @SerializedName("leaderboard_detail_url_template")
        val leaderboardDetailUrlTemplate: String? = null,
        @SerializedName("pubnub_heartbeat_interval")
        val pubnubHeartbeatInterval: Int,
        @SerializedName("pubnub_presence_timeout")
        val pubnubPresenceTimeout: Int,
        @SerializedName("badges_url")
        val badgesUrl: String,
        @SerializedName("reward_items_url")
        internal var rewardItemsUrl: String?,
        @SerializedName("user_search_url")
        val userSearchUrl: String,
        @SerializedName("chat_rooms_invitations_url")
        val chatRoomsInvitationsUrl: String,
        @SerializedName("chat_room_invitation_detail_url_template")
        val chatRoomInvitationDetailUrlTemplate: String,
        @SerializedName("create_chat_room_invitation_url")
        val createChatRoomInvitationUrl: String,
        @SerializedName("profile_chat_room_received_invitations_url_template")
        val profileChatRoomReceivedInvitationsUrlTemplate: String,
        @SerializedName("profile_chat_room_sent_invitations_url_template")
        val profileChatRoomSentInvitationsUrlTemplate: String,
    )

    companion object {
        @JvmStatic
        var enableDebug: Boolean = false

        @JvmStatic
        var predictionWidgetVoteRepository: PredictionWidgetVoteRepository =
            LocalPredictionWidgetVoteRepository()
    }
}
