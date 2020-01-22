package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.proxies.MessagingClientProxy
import com.livelike.engagementsdk.utils.gson

internal class ChatQueue(upstream: MessagingClient) :
    MessagingClientProxy(upstream),
    ChatEventListener {

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        upstream.stop()
    }

    override fun start() {
        upstream.start()
    }

    var renderer: ChatRenderer? = null

    override fun onChatMessageSend(message: ChatMessage, timeData: EpochTime) {
        publishMessage(gson.toJson(message), message.channel, timeData)
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        when (event.message.get("event").asString) {
            ChatViewModel.EVENT_NEW_MESSAGE -> {
                val chatMessage = gson.fromJson(event.message, ChatMessage::class.java)
                renderer?.displayChatMessage(chatMessage)
            }
            ChatViewModel.EVENT_MESSAGE_DELETED -> {
                val id = event.message.get("id").asString
                renderer?.deleteChatMessage(id)
            }
            ChatViewModel.EVENT_MESSAGE_TIMETOKEN_UPDATED -> {
                renderer?.updateChatMessageTimeToken(event.message.get("messageId").asString, event.message.get("timetoken").asString)
            }
            ChatViewModel.EVENT_LOADING_COMPLETE -> {
                renderer?.loadingCompleted()
            }
            ChatViewModel.EVENT_REACTION_ADDED -> {
                event.message.run {
                    renderer?.addMessageReaction(get("isOwnReaction").asBoolean, get("messagePubnubToken").asLong,
                        ChatMessageReaction(get("emojiId").asString, get("actionPubnubToken").asString.toLongOrNull()))
                }
            }
            ChatViewModel.EVENT_REACTION_REMOVED -> {
                event.message.run {
                    renderer?.removeMessageReaction(get("messagePubnubToken").asLong, get("emojiId").asString)
                }
            }
        }
    }
}

internal fun MessagingClient.toChatQueue(): ChatQueue {
    return ChatQueue(this)
}
