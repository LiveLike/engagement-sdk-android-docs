package com.livelike.engagementsdk.chat.data.repository

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.TEMPLATE_CHAT_ROOM_ID
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.core.data.respository.BaseRepository
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.chat.services.messaging.pubnub.PubnubChatMessagingClient
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result

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
        pubnubChatMessagingClient?.loadMessagesWithReactions(channel, time, 20)
    }
}
