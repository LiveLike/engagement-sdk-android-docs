package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.pubnub.PubnubChatMessagingClient

internal class ChatRepository(
    private val subscribeKey: String,
    private val authKey: String,
    private val uuid: String,
    private val analyticsService: AnalyticsService,
    private val msgListener: MessageListener
) {

    fun establishChatMessagingConnection(): MessagingClient {
        return PubnubChatMessagingClient(
            subscribeKey,
            authKey,
            uuid,
            analyticsService,
            msgListener = msgListener
        )
    }
}
