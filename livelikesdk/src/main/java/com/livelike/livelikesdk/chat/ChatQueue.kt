package com.livelike.livelikesdk.chat

import android.content.Context
import com.google.gson.JsonObject
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.messaging.sendbird.ChatClient

class ChatQueue (upstream: MessagingClient, val chatClient: ChatClient): MessagingClientProxy(upstream), ChatEventListener {
    private var connectedChannels : MutableList<String> = mutableListOf()

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
        connectedChannels = mutableListOf()
        super.unsubscribeAll()
    }

    override fun onChatMessageSend(message: ChatMessage, timeData: EpochTime) {
        //If chat is paused we can queue here

        val messageJson = JsonObject()
        messageJson.addProperty("message", message.message)
        messageJson.addProperty("sender", message.senderDisplayName)
        messageJson.addProperty("sender_id",message.senderId)
        //send on all connected channels for now, implement channel selection down the road
        connectedChannels.forEach {
            chatClient.sendMessage(ClientMessage(messageJson, it, timeData))
        }
    }

    override fun onAnalyticsEvent(data: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        //send message to chat view
        renderer?.displayChatMessage(
            ChatMessage(
                event.message.get("message").asString,
                event.message.get("sender_id").asString,
                event.message.get("sender").asString,
                event.message.get("id").asString
            )
        )
    }

    fun toggleEmission(pause: Boolean) {
        //TODO: implement pause / resume for chat
    }
}

interface ChatEventListener {
    fun onAnalyticsEvent(data: Any)
    fun onChatMessageSend(message: ChatMessage, timeData: EpochTime)
}

interface ChatRenderer {
    var chatListener: ChatEventListener?
    val chatContext: Context
    fun displayChatMessage(message: ChatMessage)
}


fun MessagingClient.toChatQueue(chatClient: ChatClient) : ChatQueue {
    val chatQueue = ChatQueue(this, chatClient)
    return chatQueue
}