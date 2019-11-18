package com.livelike.engagementsdk.chat

import com.google.gson.JsonObject
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.proxies.MessagingClientProxy
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.logError
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Date

internal class ChatQueue(upstream: MessagingClient) :
    MessagingClientProxy(upstream),
    ChatEventListener {
    override fun publishMessage(message: String,channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        upstream.stop()
    }

    override fun resume() {
        upstream.resume()
    }

    private val connectedChannels: MutableSet<String> = mutableSetOf()

    var renderer: ChatRenderer? = null

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
        messageJson.addProperty("id", message.id)
        messageJson.addProperty("channel", message.channel)
        messageJson.addProperty("image_url", URLEncoder.encode(message.senderDisplayPic, "utf-8"))
        // send on all connected channels for now, implement channel selection down the road
        connectedChannels.forEach {
            publishMessage(gson.toJson(message), it, timeData)
        }
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        when (event.message.get("event").asString) {
            ChatViewModel.EVENT_NEW_MESSAGE -> {
                val newMessage = ChatMessage(
                    event.channel,
                    event.message.get("message").asString,
                    event.message.get("sender_id").asString,
                    event.message.get("sender").asString,
                    URLDecoder.decode( event.message.get("image_url")?.asString?:"", "utf-8"),
                    event.message.get("id").asString,
                    Date(event.timeStamp.timeSinceEpochInMs).toString()
                )
                renderer?.displayChatMessage(newMessage)
            }
            ChatViewModel.EVENT_MESSAGE_DELETED -> {
                val id = event.message.get("id").asString
                renderer?.deleteChatMessage(id)
            }
            ChatViewModel.EVENT_MESSAGE_ID_UPDATED -> {
                renderer?.updateChatMessageId(event.message.get("old-id").asString, event.message.get("new-id").asString)
            }
            ChatViewModel.EVENT_LOADING_COMPLETE -> {
                renderer?.loadingCompleted()
            }
        }
    }
}

internal fun MessagingClient.toChatQueue(): ChatQueue {
    return ChatQueue(this)
}
