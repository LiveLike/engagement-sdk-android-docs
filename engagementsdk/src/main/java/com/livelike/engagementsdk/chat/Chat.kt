package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import java.util.UUID

internal interface ChatEventListener {
    fun onChatMessageSend(message: ChatMessage, timeData: EpochTime)
}

internal interface ChatRenderer {
    fun displayChatMessage(message: ChatMessage)
    fun deleteChatMessage(messageId: String)
    fun updateChatMessageTimeToken(messageId: String, timetoken: String)
    fun loadingCompleted()
    fun addMessageReaction(isOwnReaction: Boolean, messagePubnubToken: Long, chatMessageReaction: ChatMessageReaction)
    fun removeMessageReaction(messagePubnubToken: Long, emojiId: String)
}

/**
 *  Represents a chat message.
 *  @param message The message user wants to send.
 *  @param senderId This is unique user id.
 *  @param senderDisplayName This is display name user is associated with.
 *  @param id A unique ID to identify the message.
 *  @param timeStamp Message timeStamp.
 */
internal data class ChatMessage(
    var messageEvent: PubnubChatEventType,
    var channel: String,
    var message: String,
    val senderId: String,
    val senderDisplayName: String,
    val senderDisplayPic: String?,
    var id: String = UUID.randomUUID().toString(),
    // PDT video time //NOt using right now for later use FYI @shivansh @Willis
    val timeStamp: String? = null,
    var pubnubMessageToken: Long? = null,
    var imageUrl: String? = null,
    var badgeUrlImage: String? = null,
    var isFromMe: Boolean = false,
    var myChatMessageReaction: ChatMessageReaction? = null,
    var emojiCountMap: MutableMap<String, Int> = mutableMapOf(),
    // time of the message
    var timetoken: Long = 0L
) {
    fun toReportMessageJson(): String {
        return """{
                    "channel": "$channel",
                    "user_id": "$senderId",
                    "nickname": "$senderDisplayName",
                    "message_id": "$id",
                    "message": "$message"
                }""".trimIndent()
    }
}

internal data class ChatMessageReaction(
    val emojiId: String,
    var pubnubActionToken: Long? = null
)
internal const val CHAT_MESSAGE_IMAGE_TEMPLATE = ":message:"
