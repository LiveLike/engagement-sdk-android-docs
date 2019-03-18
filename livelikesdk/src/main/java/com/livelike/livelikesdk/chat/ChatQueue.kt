package com.livelike.livelikesdk.chat

import android.content.Context
import com.google.gson.JsonObject
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.messaging.sendbird.ChatClient
import com.livelike.livelikesdk.util.extractStringOrEmpty
import java.util.*

class ChatQueue (upstream: MessagingClient, val chatClient: ChatClient): MessagingClientProxy(upstream), ChatEventListener {
    private val connectedChannels : MutableList<String> = mutableListOf()

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
        val controlMessage = event.message.extractStringOrEmpty("control")

        when(controlMessage) {
            ("load_complete") -> renderer?.loadComplete()
            else -> renderer?.displayChatMessage(
                ChatMessage(
                    event.message.get("message").asString,
                    event.message.get("sender_id").asString,
                    event.message.get("sender").asString,
                    event.message.get("id").asString,
                    Date(event.timeStamp.timeSinceEpochInMs).toString()
                )
            )
        }
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
    fun loadComplete()
}


fun MessagingClient.toChatQueue(chatClient: ChatClient) : ChatQueue {
    return ChatQueue(this, chatClient)
}