package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.pubnub.PubnubChatMessagingClient

internal class ChatRepository(
    private val subscribeKey: String,
    private val publishKey: String,
    private val uuid: String,
    private val analyticsService: AnalyticsService
) {

    fun establishChatMessagingConnection(): MessagingClient {
        return PubnubChatMessagingClient(
            subscribeKey,
            uuid,
            analyticsService
        )
    }
}
