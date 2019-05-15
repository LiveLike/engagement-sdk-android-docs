package com.livelike.livelikesdk.services.messaging.sendbird

import com.livelike.livelikesdk.services.messaging.ClientMessage

internal interface ChatClient {
    var messageHandler: ChatClientResultHandler?
    fun sendMessage(message: ClientMessage)
    fun updateMessage(message: ClientMessage)
    fun deleteMessage(message: ClientMessage)
    fun updateMessagesSinceMessage(messageId: String, channel: String)
}

internal interface ChatClientResultHandler {
    fun handleMessages(messages: List<ClientMessage>)
}