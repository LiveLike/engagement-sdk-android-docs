package com.livelike.engagementsdk.chat.data.repository

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.TEMPLATE_CHAT_ROOM_ID
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMemberListResponse
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.UserChatRoomListResponse
import com.livelike.engagementsdk.chat.services.messaging.pubnub.PubnubChatMessagingClient
import com.livelike.engagementsdk.core.data.respository.BaseRepository
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.logDebug
import okhttp3.MediaType
import okhttp3.RequestBody

internal class ChatRepository(
    private val subscribeKey: String,
    private val authKey: String,
    private val uuid: String,
    private val analyticsService: AnalyticsService,
    private val publishKey: String? = null,
    private val origin: String? = null
) : BaseRepository() {

    var pubnubChatMessagingClient: PubnubChatMessagingClient? = null

    @Synchronized
    fun establishChatMessagingConnection(): MessagingClient {
        if (pubnubChatMessagingClient == null)
            pubnubChatMessagingClient =
                PubnubChatMessagingClient(
                    subscribeKey,
                    authKey,
                    uuid,
                    analyticsService,
                    publishKey,
                    origin = origin
                )
        return pubnubChatMessagingClient!!
    }

    suspend fun fetchChatRoom(chatRoomId: String, chatRoomTemplateUrl: String): Result<ChatRoom> {
        val remoteURL = chatRoomTemplateUrl.replace(TEMPLATE_CHAT_ROOM_ID, chatRoomId)
        return dataClient.remoteCall<ChatRoom>(remoteURL, RequestType.GET, accessToken = null)
    }

    suspend fun createChatRoom(
        title: String?,
        chatRoomTemplateUrl: String
    ): Result<ChatRoom> {
        val remoteURL = chatRoomTemplateUrl.replace(TEMPLATE_CHAT_ROOM_ID, "")
        val titleRequest = when (title.isNullOrEmpty()) {
            true -> RequestBody.create(null, byteArrayOf())
            else -> RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), """{"title":"$title"}"""
            )
        }
        return dataClient.remoteCall<ChatRoom>(
            remoteURL,
            RequestType.POST,
            accessToken = authKey,
            requestBody = titleRequest
        )
    }

    suspend fun addCurrentUserToChatRoom(
        chatRoomId: String,
        chatRoomTemplateUrl: String
    ): Result<ChatRoomMembership> {
        val remoteURL =
            chatRoomTemplateUrl.replace(TEMPLATE_CHAT_ROOM_ID, "") + "${chatRoomId}/memberships"
        return dataClient.remoteCall<ChatRoomMembership>(
            remoteURL,
            accessToken = authKey,
            requestType = RequestType.POST,
            requestBody = RequestBody.create(null, byteArrayOf())
        )
    }

    suspend fun getCurrentUserChatRoomList(membershipUrl: String): Result<UserChatRoomListResponse> {
        return dataClient.remoteCall<UserChatRoomListResponse>(
            membershipUrl,
            accessToken = authKey,
            requestType = RequestType.GET
        )
    }

    suspend fun getMembersOfChatRoom(
        chatRoomId: String,
        chatRoomTemplateUrl: String,
        paginationUrl: String? = null
    ): Result<ChatRoomMemberListResponse> {
        val remoteURL = paginationUrl ?: chatRoomTemplateUrl.replace(
            TEMPLATE_CHAT_ROOM_ID,
            ""
        ) + "${chatRoomId}/memberships"
        return dataClient.remoteCall<ChatRoomMemberListResponse>(
            remoteURL,
            accessToken = authKey,
            requestType = RequestType.GET
        )
    }

    suspend fun deleteCurrentUserFromChatRoom(
        membershipId: String,
        chatRoomTemplateUrl: String
    ): Result<Void> {
        val remoteURL =
            chatRoomTemplateUrl.replace(
                TEMPLATE_CHAT_ROOM_ID,
                ""
            ) + "/chat-room-memberships/$membershipId"
        return dataClient.remoteCall<Void>(
            remoteURL,
            accessToken = authKey,
            requestType = RequestType.DELETE
        )
    }

    fun addMessageReaction(channel: String, messagePubnubToken: Long, emojiId: String) {
        pubnubChatMessagingClient?.addMessageAction(channel, messagePubnubToken, emojiId)
    }

    fun removeMessageReaction(
        channel: String,
        messagePubnubToken: Long,
        reactionpubnubToken: Long
    ) {
        pubnubChatMessagingClient?.removeMessageAction(
            channel,
            messagePubnubToken,
            reactionpubnubToken
        )
    }

    fun loadPreviousMessages(channel: String, time: Long) {
        logDebug { "ChatRepository.loadPreviousMessages time:$time" }
        pubnubChatMessagingClient?.loadMessagesWithReactions(channel, time, 20)
    }
}
