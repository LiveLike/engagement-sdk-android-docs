package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.MessageListener
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
    private val msgListener: MessageListener
) : BaseRepository() {

    fun establishChatMessagingConnection(): MessagingClient {
        return PubnubChatMessagingClient(
            subscribeKey,
            authKey,
            uuid,
            analyticsService,
            msgListener = msgListener
        )
    }

    suspend fun fetchChatRoom(chatRoomId: String, chatRoomTemplateUrl: String): Result<ChatRoom> {
        val remoteURL = chatRoomTemplateUrl.replace("{chat_room_id}", chatRoomId)
        return dataClient.remoteCall<ChatRoom>(remoteURL, RequestType.GET, accessToken = null)
    }
}
