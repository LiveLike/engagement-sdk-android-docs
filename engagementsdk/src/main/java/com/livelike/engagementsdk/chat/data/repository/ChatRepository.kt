package com.livelike.engagementsdk.chat.data.repository

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.CHAT_HISTORY_LIMIT
import com.livelike.engagementsdk.TEMPLATE_CHAT_ROOM_ID
import com.livelike.engagementsdk.chat.ChatRoomRequest
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.chat.data.remote.*
import com.livelike.engagementsdk.chat.services.messaging.pubnub.PubnubChatMessagingClient
import com.livelike.engagementsdk.core.data.respository.BaseRepository
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.publicapis.LiveLikeEmptyResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

internal class ChatRepository(
    private val subscribeKey: String,
    private val authKey: String,
    private val uuid: String,
    private val analyticsService: AnalyticsService,
    private val publishKey: String? = null,
    private val origin: String? = null,
    private val pubnubHeartbeatInterval: Int,
    private val pubnubPresenceTimeout: Int
) : BaseRepository() {

    var pubnubChatMessagingClient: PubnubChatMessagingClient? = null

    @Synchronized
    fun establishChatMessagingConnection(): MessagingClient {
        if (pubnubChatMessagingClient == null)
            pubnubChatMessagingClient =
                PubnubChatMessagingClient(
                    subscriberKey = subscribeKey,
                    authKey = authKey,
                    uuid = uuid,
                    analyticsService = analyticsService,
                    publishKey = publishKey,
                    origin = origin,
                    pubnubHeartbeatInterval = pubnubHeartbeatInterval,
                    pubnubPresenceTimeout = pubnubPresenceTimeout,
                    chatRepository = this
                )
        return pubnubChatMessagingClient!!
    }

    suspend fun fetchChatRoom(chatRoomId: String, chatRoomTemplateUrl: String): Result<ChatRoom> {
        val remoteURL = chatRoomTemplateUrl.replace(TEMPLATE_CHAT_ROOM_ID, chatRoomId)
        return dataClient.remoteCall<ChatRoom>(remoteURL, RequestType.GET, accessToken = null)
    }

    suspend fun createChatRoom(
        title: String?,
        visibility: Visibility?,
        chatRoomTemplateUrl: String
    ): Result<ChatRoom> {
        val remoteURL = chatRoomTemplateUrl.replace(TEMPLATE_CHAT_ROOM_ID, "")
        val titleRequest = gson.toJson(ChatRoomRequest(title, visibility))
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        return dataClient.remoteCall<ChatRoom>(
            remoteURL,
            RequestType.POST,
            accessToken = authKey,
            requestBody = titleRequest
        )
    }

    suspend fun updateChatRoom(
        title: String?,
        visibility: Visibility?,
        chatRoomId: String,
        chatRoomTemplateUrl: String
    ): Result<ChatRoom> {
        val chatRoomResult = fetchChatRoom(chatRoomId, chatRoomTemplateUrl)
        val titleRequest = gson.toJson(ChatRoomRequest(title, visibility))
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        return if (chatRoomResult is Result.Success) {
            return dataClient.remoteCall<ChatRoom>(
                chatRoomResult.data.url,
                RequestType.PUT,
                accessToken = authKey,
                requestBody = titleRequest
            )
        } else {
            chatRoomResult as Result.Error
        }
    }

    suspend fun getCurrentUserChatRoomList(membershipUrl: String): Result<UserChatRoomListResponse> {
        return dataClient.remoteCall<UserChatRoomListResponse>(
            membershipUrl,
            accessToken = authKey,
            requestType = RequestType.GET
        )
    }

    suspend fun deleteCurrentUserFromChatRoom(
        chatRoomId: String,
        chatRoomTemplateUrl: String
    ): Result<LiveLikeEmptyResponse> {
        val chatRoomResult = fetchChatRoom(chatRoomId, chatRoomTemplateUrl)
        return if (chatRoomResult is Result.Success) {
            dataClient.remoteCall<LiveLikeEmptyResponse>(
                chatRoomResult.data.membershipsUrl,
                accessToken = authKey,
                requestType = RequestType.DELETE
            )
        } else {
            chatRoomResult as Result.Error
        }
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

    fun loadPreviousMessages(limit: Int = CHAT_HISTORY_LIMIT) {
//        pubnubChatMessagingClient?.loadMessagesWithReactions(
//            channel,
//            limit
//        )
        pubnubChatMessagingClient?.loadMessagesWithReactionsFromServer(limit)
    }

    suspend fun sendMessage(
        url: String,
        pubnubChatMessage: PubnubChatMessage
    ): Result<PubnubChatMessage> {
        return dataClient.remoteCall(
            url,
            accessToken = authKey,
            requestType = RequestType.POST,
            requestBody = gson.toJson(pubnubChatMessage).toRequestBody(
                "application/json; charset=utf-8".toMediaTypeOrNull()
            )
        )
    }

    suspend fun getMessageHistory(
        url: String,
        chatRoomId: String,
        since: String? = null,
        until: String? = null,
        pageSize: Int = CHAT_HISTORY_LIMIT
    ): Result<PubnubChatListResponse> {
        var apiUrl = "$url?chat_room_id=$chatRoomId&page_size=$pageSize"
        since?.let {
            apiUrl = "$apiUrl&since=${URLEncoder.encode(it, "utf-8")}"
        }
        until?.let {
            apiUrl = "$apiUrl&until=${URLEncoder.encode(it, "utf-8")}"
        }

        return dataClient.remoteCall(
            apiUrl,
            accessToken = authKey,
            requestType = RequestType.GET
        )
    }

    suspend fun getMessageCount(
        url: String, since: String? = null,
        until: String? = null
    ): Result<PubnubChatListCountResponse> {
        var apiUrl = "$url?"
        since?.let {
            apiUrl = "$apiUrl&since=${URLEncoder.encode(it, "utf-8")}"
        }
        until?.let {
            apiUrl = "$apiUrl&until=${URLEncoder.encode(it, "utf-8")}"
        }
        return dataClient.remoteCall(apiUrl, accessToken = authKey, requestType = RequestType.GET)
    }

    suspend fun postApi(url: String, customData: String): Result<LiveLikeChatMessage> {
        return dataClient.remoteCall(
            url,
            accessToken = authKey,
            requestType = RequestType.POST,
            requestBody = customData.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
        )
    }
}
