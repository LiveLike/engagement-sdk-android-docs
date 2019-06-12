package com.livelike.livelikesdk.chat

import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.ChatEventListener
import com.livelike.engagementsdkapi.ChatMessage
import com.livelike.engagementsdkapi.ChatRenderer
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.services.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.services.messaging.sendbird.ChatClient
import java.util.Date

internal class ChatQueue(upstream: MessagingClient, private val chatClient: ChatClient) :
    MessagingClientProxy(upstream),
    ChatEventListener {
    private val connectedChannels: MutableList<String> = mutableListOf()
    private var lastChatMessage: Pair<String, String>? = null
    private var paused = false

    var renderer: ChatRenderer? = null
        set(value) {
            field = value
            value?.chatListener = this
        }

    override fun subscribe(channels: List<String>) {
        connectedChannels.addAll(channels)
        upstream.subscribe(channels)
    }

    override fun unsubscribe(channels: List<String>) {
        channels.forEach { connectedChannels.remove(it) }
        super.unsubscribe(channels)
    }

    override fun unsubscribeAll() {
        connectedChannels.clear()
        super.unsubscribeAll()
    }

    override fun onChatMessageSend(message: ChatMessage, timeData: EpochTime) {
        // If chat is paused we can queue here
        val messageJson = JsonObject()
        messageJson.addProperty("message", message.message)
        messageJson.addProperty("sender", message.senderDisplayName)
        messageJson.addProperty("sender_id", message.senderId)
        // send on all connected channels for now, implement channel selection down the road
        connectedChannels.forEach {
            chatClient.sendMessage(ClientMessage(messageJson, it, timeData))
        }
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        val newMessage = ChatMessage(
            event.message.get("message").asString,
            event.message.get("sender_id").asString,
            event.message.get("sender").asString,
            event.message.get("id").asString,
            Date(event.timeStamp.timeSinceEpochInMs).toString()
        )
        if (!paused) {
            renderer?.displayChatMessage(newMessage)
            lastChatMessage = Pair(newMessage.id, event.channel)
        }
    }

    fun toggleEmission(pause: Boolean) {
        paused = pause
        val lastMessage = lastChatMessage
        if (!paused && lastMessage != null)
            chatClient.updateMessagesSinceMessage(lastMessage.first, lastMessage.second)
    }
}

internal fun MessagingClient.toChatQueue(chatClient: ChatClient): ChatQueue {
    return ChatQueue(this, chatClient)
}