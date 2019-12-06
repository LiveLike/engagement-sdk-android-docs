package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.EpochTime
import java.util.UUID

interface ChatEventListener {
    fun onChatMessageSend(message: ChatMessage, timeData: EpochTime)
}

interface ChatRenderer {
    fun displayChatMessage(message: ChatMessage)
    fun deleteChatMessage(messageId: String)
    fun updateChatMessageId(oldId: String, newId: String)
    fun loadingCompleted()
}

/**
 *  Represents a chat message.
 *  @param message The message user wants to send.
 *  @param senderId This is unique user id.
 *  @param senderDisplayName This is display name user is associated with.
 *  @param id A unique ID to identify the message.
 *  @param timeStamp Message timeStamp.
 */
data class ChatMessage(
    var channel: String,
    var message: String,
    val senderId: String,
    val senderDisplayName: String,
    val senderDisplayPic: String?,
    var id: String = UUID.randomUUID().toString(),
    val timeStamp: String? = null,
    var isFromMe: Boolean = false
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
