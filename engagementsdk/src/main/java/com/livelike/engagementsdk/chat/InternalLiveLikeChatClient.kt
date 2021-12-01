package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.MockAnalyticsService
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMemberListResponse
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.chat.data.remote.UserChatRoomListResponse
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.combineLatestOnce
import com.livelike.engagementsdk.publicapis.BlockedData
import com.livelike.engagementsdk.publicapis.BlockedProfileListResponse
import com.livelike.engagementsdk.publicapis.ChatRoomInvitation
import com.livelike.engagementsdk.publicapis.ChatRoomInvitationResponse
import com.livelike.engagementsdk.publicapis.ChatRoomInvitationStatus
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeEmptyResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

internal class InternalLiveLikeChatClient(
    private val configurationStream: Stream<EngagementSDK.SdkConfiguration>,
    private val userRepository: UserRepository,
    private val uiScope: CoroutineScope,
    private val dataClient: EngagementDataClientImpl,
) : LiveLikeChatClient {
    private var userChatRoomListResponse: UserChatRoomListResponse? = null
    private var chatRoomMemberListMap: MutableMap<String, ChatRoomMemberListResponse> =
        mutableMapOf()
    private val invitationForProfileMap = hashMapOf<String, ChatRoomInvitationResponse>()
    private val invitationByProfileMap = hashMapOf<String, ChatRoomInvitationResponse>()
    private var blockedProfileListResponseMap = hashMapOf<String, BlockedProfileListResponse>()


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
                        when (chatRoomResult) {
                            is Result.Success -> {
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
                            }
                            is Result.Error -> {
                                liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                            }
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
                        when (chatRoomResult) {
                            is Result.Success -> {
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
                            }
                            is Result.Error -> {
                                liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                            }
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
                        when (chatRoomResult) {
                            is Result.Success -> {
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
                                when (currentUserChatRoomResult) {
                                    is Result.Success -> {
                                        liveLikeCallback.onResponse(
                                            currentUserChatRoomResult.data, null
                                        )
                                    }
                                    is Result.Error -> {
                                        liveLikeCallback.onResponse(
                                            null,
                                            currentUserChatRoomResult.exception.message
                                        )
                                    }
                                }
                            }
                            is Result.Error -> {
                                liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                            }
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
                        when {
                            url != null -> {
                                val chatRoomResult = chatRepository.getCurrentUserChatRoomList(
                                    url
                                )
                                when (chatRoomResult) {
                                    is Result.Success -> {
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
                                    }
                                    is Result.Error -> {
                                        liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                                    }
                                }
                            }
                            else -> {
                                liveLikeCallback.onResponse(null, "No More data to load")
                            }
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
                        when (chatRoomResult) {
                            is Result.Success -> {
                                val url = when (liveLikePagination) {
                                    LiveLikePagination.FIRST -> chatRoomResult.data.membershipsUrl
                                    LiveLikePagination.NEXT -> chatRoomMemberListMap[chatRoomId]?.next
                                    LiveLikePagination.PREVIOUS -> chatRoomMemberListMap[chatRoomId]?.previous
                                }
                                when {
                                    url != null -> {
                                        val chatRoomMemberResult =
                                            dataClient.remoteCall<ChatRoomMemberListResponse>(
                                                url,
                                                accessToken = pair.first.accessToken,
                                                requestType = RequestType.GET
                                            )
                                        when (chatRoomMemberResult) {
                                            is Result.Success -> {
                                                chatRoomMemberListMap[chatRoomId] = chatRoomMemberResult.data
                                                val list = chatRoomMemberResult.data.results?.map {
                                                    it?.profile!!
                                                }
                                                liveLikeCallback.onResponse(list, null)
                                            }
                                            is Result.Error -> {
                                                liveLikeCallback.onResponse(
                                                    null,
                                                    chatRoomMemberResult.exception.message
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        liveLikeCallback.onResponse(null, "No More data to load")
                                    }
                                }
                            }
                            is Result.Error -> {
                                liveLikeCallback.onResponse(null, chatRoomResult.exception.message)
                            }
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
                        val chatRoomResult = chatRepository.deleteCurrentUserFromChatRoom(
                            chatRoomId, pair.second.chatRoomDetailUrlTemplate
                        )
                        liveLikeCallback.processResult(chatRoomResult)
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
                            userRepository.userAccessToken, true
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
                userRepository.userAccessToken,
                true
            )
            liveLikeCallback.processResult(result)
        }
    }

    override fun getInvitationsReceivedByCurrentProfileWithInvitationStatus(
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
                        when {
                            url != null -> {
                                val result = dataClient.remoteCall<ChatRoomInvitationResponse>(
                                    url,
                                    RequestType.GET,
                                    requestBody = null,
                                    userRepository.userAccessToken, true
                                )
                                when (result) {
                                    is Result.Success -> {
                                        invitationForProfileMap[it.first.id] = result.data
                                        liveLikeCallback.onResponse(result.data.results, null)
                                    }
                                    is Result.Error -> {
                                        liveLikeCallback.onResponse(
                                            null,
                                            result.exception.message
                                        )
                                    }
                                }
                            }
                            else -> {
                                liveLikeCallback.onResponse(null, "No More data to load")
                            }
                        }
                    }
                }
            }
    }

    override fun getInvitationsSentByCurrentProfileWithInvitationStatus(
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
                        when {
                            url != null -> {
                                val result = dataClient.remoteCall<ChatRoomInvitationResponse>(
                                    url,
                                    RequestType.GET,
                                    requestBody = null,
                                    userRepository.userAccessToken, true
                                )
                                when (result) {
                                    is Result.Success -> {
                                        invitationByProfileMap[it.first.id] = result.data
                                        liveLikeCallback.onResponse(result.data.results, null)
                                    }
                                    is Result.Error -> {
                                        liveLikeCallback.onResponse(
                                            null,
                                            result.exception.message
                                        )
                                    }
                                }
                            }
                            else -> {
                                liveLikeCallback.onResponse(null, "No More data to load")
                            }
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
                        userRepository.userAccessToken,
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
                        userRepository.userAccessToken,
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
                    when {
                        url != null -> {
                            val result = dataClient.remoteCall<BlockedProfileListResponse>(
                                url,
                                RequestType.GET,
                                requestBody = null,
                                userRepository.userAccessToken, true
                            )

                            when (result) {
                                is Result.Success -> {
                                    blockedProfileListResponseMap[params] = result.data
                                    liveLikeCallback.onResponse(result.data.results, null)
                                }
                                is Result.Error -> {
                                    liveLikeCallback.onResponse(
                                        null,
                                        result.exception.message
                                    )
                                }
                            }
                        }
                        else -> {
                            liveLikeCallback.onResponse(null, "No More data to load")
                        }
                    }
                }
            }
        }
    }

    internal suspend fun getChatRoom(
        configurationUserPairFlow: Flow<Pair<LiveLikeUser, EngagementSDK.SdkConfiguration>>,
        chatRoomId: String
    ): Flow<Result<ChatRoom>> {
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
}