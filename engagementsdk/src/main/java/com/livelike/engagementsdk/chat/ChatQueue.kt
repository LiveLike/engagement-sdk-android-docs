package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.proxies.MessagingClientProxy
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.publicapis.toLiveLikeChatMessage
<<<<<<< Updated upstream
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.proxies.MessagingClientProxy
import com.livelike.engagementsdk.core.utils.gson
=======
>>>>>>> Stashed changes

internal class ChatQueue(upstream: MessagingClient) :
    MessagingClientProxy(upstream),
    ChatEventListener {

    var msgListener: MessageListener? = null
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
                chatMessage.timeStamp = event.timeStamp.timeSinceEpochInMs.toString()
                renderer?.displayChatMessage(chatMessage)
                msgListener?.onNewMessage(
                    event.channel,
                    chatMessage.toLiveLikeChatMessage()
                )
            }
            ChatViewModel.EVENT_MESSAGE_DELETED -> {
                val id = event.message.get("id").asString
                renderer?.deleteChatMessage(id)
            }
            ChatViewModel.EVENT_MESSAGE_TIMETOKEN_UPDATED -> {
                renderer?.updateChatMessageTimeToken(
                    event.message.get("messageId").asString,
                    event.message.get("timetoken").asString
                )
                var epochTimeStamp = 0L
                val time = event.message.get("timetoken").asString.toLong()
                if (time > 0) {
                    epochTimeStamp = time / 10000
                }
                msgListener?.onNewMessage(
                    event.channel,
                    LiveLikeChatMessage(
                        "",
                        "",
                        "",
                        epochTimeStamp.toString(),
                        event.message.get("messageId").asString.hashCode().toLong()
                    )
                )
            }
            ChatViewModel.EVENT_LOADING_COMPLETE -> {
                renderer?.loadingCompleted()
            }
            ChatViewModel.EVENT_REACTION_ADDED -> {
                event.message.run {
                    renderer?.addMessageReaction(
                        get("isOwnReaction").asBoolean, get("messagePubnubToken").asLong,
                        ChatMessageReaction(
                            get("emojiId").asString,
                            get("actionPubnubToken").asString.toLongOrNull()
                        )
                    )
                }
            }
            ChatViewModel.EVENT_REACTION_REMOVED -> {
                event.message.run {
                    renderer?.removeMessageReaction(
                        get("messagePubnubToken").asLong,
                        get("emojiId").asString
                    )
                }
            }
        }
    }
}

internal fun MessagingClient.toChatQueue(): ChatQueue {
    return ChatQueue(this)
}
