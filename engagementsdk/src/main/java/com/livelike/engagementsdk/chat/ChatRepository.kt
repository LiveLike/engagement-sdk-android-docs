package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.TEMPLATE_CHAT_ROOM_ID
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.data.repository.BaseRepository
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.pubnub.PubnubChatMessagingClient
import com.livelike.engagementsdk.services.network.RequestType
import com.livelike.engagementsdk.services.network.Result

internal class ChatRepository(
    private val subscribeKey: String,
    private val authKey: String,
    private val uuid: String,
    private val analyticsService: AnalyticsService,
    private val msgListener: MessageListener,
    private val publishKey: String? = null
) : BaseRepository() {

    var pubnubChatMessagingClient : PubnubChatMessagingClient? = null

    fun establishChatMessagingConnection(): MessagingClient {
        pubnubChatMessagingClient = PubnubChatMessagingClient(
            subscribeKey,
            authKey,
            uuid,
            analyticsService,
            publishKey,
            msgListener = msgListener
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

    fun loadPreviousMessages(channel: String, time: Long){
        println("ChatRepository.loadPreviousMessages-> $time")
        pubnubChatMessagingClient?.loadMessageHistory(channel, time, 20)
    }
}
