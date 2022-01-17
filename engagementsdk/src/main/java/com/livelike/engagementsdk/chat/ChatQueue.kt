package com.livelike.engagementsdk.chat

import com.example.example.PinMessageInfo
import com.livelike.engagementsdk.ChatRoomListener
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.Error
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.proxies.MessagingClientProxy
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.publicapis.*

internal class ChatQueue(
    upstream: MessagingClient,
) :
    MessagingClientProxy(upstream),
    ChatEventListener {
    var msgListener: MessageListener? = null
    var chatRoomListener: ChatRoomListener? = null
    var blockedProfileIds = hashSetOf<String>()
    var liveLikeChatClient: LiveLikeChatClient? = null
        set(value) {
            field = value
            value?.apply {
                getProfileBlockIds(object : LiveLikeCallback<List<String>>() {
                    override fun onResponse(result: List<String>?, error: String?) {
                        error?.let {
                            logError { "Block Profile Ids Error: $it" }
                        }
                        result?.let {
                            blockedProfileIds.addAll(it)
                        }
                    }
                })
                (this as InternalLiveLikeChatClient).subscribeToChatRoomInternalDelegate(
                    this.hashCode().toString(), object : ChatRoomDelegate() {
                        override fun onNewChatRoomAdded(chatRoomAdd: ChatRoomAdd) {

                        }

                        override fun onReceiveInvitation(invitation: ChatRoomInvitation) {

                        }

                        override fun onBlockProfile(blockedInfo: BlockedInfo) {
                            blockedProfileIds.add(blockedInfo.blockedProfileID)
                        }

                        override fun onUnBlockProfile(blockInfoId: String, blockProfileId: String) {
                            blockedProfileIds.remove(blockProfileId)
                        }
                    })
            }
        }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        upstream.stop()
    }

    override fun start() {
        upstream.start()
    }

    override fun onClientMessageEvents(client: MessagingClient, events: List<ClientMessage>) {
        val list = events.filter { event ->
            return@filter (event.message.get("event").asString == ChatViewModel.EVENT_NEW_MESSAGE)
        }
        val deletedList = events.filter { event ->
            return@filter (event.message.get("event").asString == ChatViewModel.EVENT_MESSAGE_DELETED)
        }

        deletedList.forEach { event ->
            val chatMessage = gson.fromJson(event.message, ChatMessage::class.java)
            chatMessage.timeStamp = event.timeStamp.timeSinceEpochInMs.toString()
            renderer?.deleteChatMessage(chatMessage.id)
            msgListener?.onDeleteMessage(chatMessage.id)
        }
        val messageList = list.map { event ->
            val chatMessage = gson.fromJson(event.message, ChatMessage::class.java)
            chatMessage.timeStamp = event.timeStamp.timeSinceEpochInMs.toString()
            return@map chatMessage
        }.filter {
            !blockedProfileIds
                .contains(it.senderId)
        }
        msgListener?.onHistoryMessage(messageList.map { it.toLiveLikeChatMessage() })
        renderer?.displayChatMessages(messageList)

    }

    var renderer: ChatRenderer? = null

    override fun onChatMessageSend(message: ChatMessage, timeData: EpochTime) {
        publishMessage(gson.toJson(message), message.channel, timeData)
    }

    override fun onClientMessageError(client: MessagingClient, error: Error) {
        super.onClientMessageError(client, error)
        if (error.type.equals(MessageError.DENIED_MESSAGE_PUBLISH.name)) {
            renderer?.errorSendingMessage(MessageError.DENIED_MESSAGE_PUBLISH)
        }
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        when (event.message.get("event").asString) {
            ChatViewModel.EVENT_NEW_MESSAGE -> {
                val chatMessage = gson.fromJson(event.message, ChatMessage::class.java)
                chatMessage.timeStamp = event.timeStamp.timeSinceEpochInMs.toString()
                if (blockedProfileIds
                        .contains(chatMessage.senderId)
                ) {
                    logDebug { "user is blocked" }
                    return
                }
                renderer?.displayChatMessage(chatMessage)
                msgListener?.onNewMessage(chatMessage.toLiveLikeChatMessage())
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
                val chatMessage = gson.fromJson(event.message, ChatMessage::class.java)
                chatMessage.timeStamp = epochTimeStamp.toString()
                msgListener?.onNewMessage(chatMessage.toLiveLikeChatMessage())
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
            PubnubChatEventType.CHATROOM_UPDATED.key -> {
                val chatRoom = gson.fromJson(event.message, ChatRoom::class.java)
                chatRoomListener?.onChatRoomUpdate(chatRoom.toLiveLikeChatRoom())
            }
            PubnubChatEventType.MESSAGE_PINNED.key -> {
                val pinMessageInfo = gson.fromJson(event.message, PinMessageInfo::class.java)
                msgListener?.onPinMessage(pinMessageInfo)
            }
            PubnubChatEventType.MESSAGE_UNPINNED.key -> {
                val pinMessageInfoId = event.message.get("id").asString
                msgListener?.onUnPinMessage(pinMessageInfoId)
            }
        }
    }
}

internal fun MessagingClient.toChatQueue(): ChatQueue {
    return ChatQueue(this)
}

internal enum class MessageError {
    DENIED_MESSAGE_PUBLISH
}
