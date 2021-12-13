package com.livelike.engagementsdk.chat

import com.example.example.PinMessageInfo
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.MockAnalyticsService
import com.livelike.engagementsdk.TEMPLATE_PROFILE_ID
import com.livelike.engagementsdk.chat.data.remote.*
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.chat.data.repository.ChatRoomRepository
import com.livelike.engagementsdk.chat.services.messaging.pubnub.PubnubChatRoomMessagingClient
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.publicapis.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

internal class InternalLiveLikeChatClient(
    private val configurationUserPairFlow: Flow<Pair<LiveLikeUser, EngagementSDK.SdkConfiguration>>,
    private val uiScope: CoroutineScope,
    private val sdkScope: CoroutineScope,
    private val dataClient: EngagementDataClientImpl,
) : LiveLikeChatClient {
    private var userChatRoomListResponse: UserChatRoomListResponse? = null
    private var chatRoomMemberListMap: MutableMap<String, ChatRoomMemberListResponse> =
        mutableMapOf()
    private val invitationForProfileMap = hashMapOf<String, ChatRoomInvitationResponse>()
    private val invitationByProfileMap = hashMapOf<String, ChatRoomInvitationResponse>()
    private var blockedProfileResponse: BlockedProfileListResponse? = null
    private var blockProfileIdsResponse: BlockProfileIdsResponse? = null
    private var internalChatRoomDelegate = hashMapOf<String, ChatRoomDelegate>()

    internal fun subscribeToChatRoomInternalDelegate(key: String, delegate: ChatRoomDelegate) {
        internalChatRoomDelegate[key] = delegate
    }

    internal fun unsubscribeToChatRoomDelegate(key: String) {
        internalChatRoomDelegate.remove(key)
    }

    override var chatRoomDelegate: ChatRoomDelegate? = null
        set(value) {
            field = value
            setUpPubNubClientForChatRoom()
        }

    internal var pubnubClient: PubnubChatRoomMessagingClient? = null

    internal fun setUpPubNubClientForChatRoom() {
        if (pubnubClient == null) {
            sdkScope.launch {
                configurationUserPairFlow.collect { pair ->
                    pubnubClient = PubnubChatRoomMessagingClient(
                        pair.second.pubNubKey,
                        pair.second.pubnubHeartbeatInterval,
                        pair.first.id,
                        pair.second.pubnubPresenceTimeout
                    )
                    pubnubClient?.chatRoomDelegate = object : ChatRoomDelegate() {
                        override fun onNewChatRoomAdded(chatRoomAdd: ChatRoomAdd) {
                            chatRoomDelegate?.onNewChatRoomAdded(chatRoomAdd)
                            internalChatRoomDelegate.values.forEach {
                                it.onNewChatRoomAdded(chatRoomAdd)
                            }
                        }

                        override fun onReceiveInvitation(invitation: ChatRoomInvitation) {
                            chatRoomDelegate?.onReceiveInvitation(invitation)
                            internalChatRoomDelegate.values.forEach {
                                it.onReceiveInvitation(invitation)
                            }
                        }

                        override fun onBlockProfile(blockedInfo: BlockedInfo) {
                            chatRoomDelegate?.onBlockProfile(blockedInfo)
                            internalChatRoomDelegate.values.forEach {
                                it.onBlockProfile(blockedInfo)
                            }
                        }

                        override fun onUnBlockProfile(blockInfoId: String, blockProfileId: String) {
                            chatRoomDelegate?.onUnBlockProfile(blockInfoId, blockProfileId)
                            internalChatRoomDelegate.values.forEach {
                                it.onUnBlockProfile(blockInfoId, blockProfileId)
                            }
                        }
                    }
                    pubnubClient?.subscribe(arrayListOf(pair.first.subscribeChannel!!))
                }
            }
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
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
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
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
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
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
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
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
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
                            liveLikeCallback.onResponse(
                                null,
                                chatRoomResult.exception.message
                            )
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
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
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
                                chatRoomMemberListMap[chatRoomId] =
                                    chatRoomMemberResult.data
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
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
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
        sdkScope.launch {
            configurationUserPairFlow.collect {
                uiScope.launch {
                    val result = dataClient.remoteCall<ChatRoomInvitation>(
                        it.second.createChatRoomInvitationUrl,
                        RequestType.POST,
                        requestBody = """{"chat_room_id":"$chatRoomId","invited_profile_id":"$profileId"}"""
                            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
                        it.first.accessToken, true
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
            configurationUserPairFlow.collect { pair ->
                val result = dataClient.remoteCall<ChatRoomInvitation>(
                    chatRoomInvitation.url,
                    RequestType.PATCH,
                    requestBody = """{"status":"${invitationStatus.key}"}"""
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
                    pair.first.accessToken,
                    true
                )
                liveLikeCallback.processResult(result)
            }
        }
    }

    override fun getInvitationsReceivedByCurrentProfileWithInvitationStatus(
        liveLikePagination: LiveLikePagination,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInvitation>>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect {
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
                            it.first.accessToken, true
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

    override fun getInvitationsSentByCurrentProfileWithInvitationStatus(
        liveLikePagination: LiveLikePagination,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInvitation>>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect {
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
                            it.first.accessToken, true
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
        liveLikeCallback: LiveLikeCallback<BlockedInfo>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect {
                it.first.blockProfileUrl?.let { url ->
                    uiScope.launch {
                        val result = dataClient.remoteCall<BlockedInfo>(
                            url,
                            RequestType.POST,
                            requestBody = """{"blocked_profile_id":"$profileId"}"""
                                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
                            it.first.accessToken,
                            true
                        )
                        liveLikeCallback.processResult(result)
                    }
                }
            }
        }
    }

    override fun unBlockProfile(
        blockId: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect {
                it.first.blockProfileUrl?.let { url ->
                    uiScope.launch {
                        val result = dataClient.remoteCall<LiveLikeEmptyResponse>(
                            "$url$blockId/",
                            RequestType.DELETE,
                            requestBody = null,
                            it.first.accessToken,
                            true
                        )
                        liveLikeCallback.processResult(result)
                    }
                }
            }
        }
    }

    override fun getBlockedProfileList(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<BlockedInfo>>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                pair.first.blockProfileListTemplate.let {
                    uiScope.launch {
                        val url = when (liveLikePagination) {
                            LiveLikePagination.FIRST -> it.replace("{blocked_profile_id}", "")
                            LiveLikePagination.NEXT -> blockedProfileResponse?.next
                            LiveLikePagination.PREVIOUS -> blockedProfileResponse?.previous
                        }
                        if (url != null) {
                            val result = dataClient.remoteCall<BlockedProfileListResponse>(
                                url,
                                RequestType.GET,
                                requestBody = null,
                                pair.first.accessToken, true
                            )

                            if (result is Result.Success) {
                                blockedProfileResponse = result.data
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
    }

    override fun getProfileMutedStatus(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatUserMuteStatus>
    ) {
        uiScope.launch {
            getChatRoom(
                chatRoomId
            ).collect {
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

    override fun getProfileBlockInfo(
        profileId: String,
        liveLikeCallback: LiveLikeCallback<BlockedInfo>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                uiScope.launch {
                    val url = pair.first.blockProfileListTemplate.replace(
                        "{blocked_profile_id}",
                        profileId
                    )
                    val result = dataClient.remoteCall<BlockedProfileListResponse>(
                        url,
                        RequestType.GET,
                        requestBody = null,
                        pair.first.accessToken, true
                    )

                    if (result is Result.Success) {
                        liveLikeCallback.onResponse(result.data.results.firstOrNull(), null)
                    } else if (result is Result.Error) {
                        liveLikeCallback.onResponse(
                            null,
                            result.exception.message
                        )
                    }
                }
            }
        }
    }

    override fun getProfileBlockIds(liveLikeCallback: LiveLikeCallback<List<String>>) {
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                uiScope.launch {
                    val result = dataClient.remoteCall<BlockProfileIdsResponse>(
                        pair.first.blockedProfileIdsUrl,
                        requestType = RequestType.GET,
                        accessToken = pair.first.accessToken,
                        fullErrorJson = true
                    )
                    if (result is Result.Success) {
                        blockProfileIdsResponse = result.data
                        liveLikeCallback.onResponse(result.data.results, null)
                    } else if (result is Result.Error) {
                        liveLikeCallback.onResponse(
                            null,
                            result.exception.message
                        )
                    }
                }
            }
        }
    }

    internal suspend fun getChatRoom(
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

    override fun pinMessage(
        messageId: String,
        chatRoomId: String,
        chatMessagePayload: LiveLikeChatMessage,
        liveLikeCallback: LiveLikeCallback<PinMessageInfo>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                uiScope.launch {
                    val result = dataClient.remoteCall<PinMessageInfo>(
                        pair.second.pinnedMessageUrl,
                        requestType = RequestType.POST,
                        requestBody = gson.toJson(
                            PinMessageInfoRequest(
                                messageId,
                                chatMessagePayload,
                                chatRoomId
                            )
                        ).toRequestBody(
                            "application/json; charset=utf-8".toMediaTypeOrNull()
                        ),
                        accessToken = pair.first.accessToken,
                        true
                    )
                    liveLikeCallback.processResult(result)
                }
            }
        }
    }

    override fun unPinMessage(
        pinMessageInfoId: String,
        liveLiveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                uiScope.launch {
                    val result = dataClient.remoteCall<LiveLikeEmptyResponse>(
                        "${pair.second.pinnedMessageUrl}$pinMessageInfoId/",
                        RequestType.DELETE,
                        accessToken = pair.first.accessToken,
                        fullErrorJson = true
                    )
                    liveLiveLikeCallback.processResult(result)
                }
            }
        }
    }

    override fun getPinMessageInfoList(
        chatRoomId: String,
        order: PinMessageOrder,
        pagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<PinMessageInfo>>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                uiScope.launch {
                    val url = when (pagination) {
                        LiveLikePagination.FIRST -> "${pair.second.pinnedMessageUrl}?chat_room_id=$chatRoomId"
                        LiveLikePagination.NEXT -> pinMessageInfoListResponse?.next
                        LiveLikePagination.PREVIOUS -> pinMessageInfoListResponse?.previous
                    }
                    if (url != null) {
                        val result = dataClient.remoteCall<PinMessageInfoListResponse>(
                            "$url${
                                when (order) {
                                    PinMessageOrder.DESC -> "&ordering=-pinned_at"
                                    else -> "&ordering=pinned_at"
                                }
                            }",
                            RequestType.GET,
                            accessToken = pair.first.accessToken,
                            fullErrorJson = true
                        )
                        if (result is Result.Success) {
                            pinMessageInfoListResponse = result.data
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

    private var pinMessageInfoListResponse: PinMessageInfoListResponse? = null

}