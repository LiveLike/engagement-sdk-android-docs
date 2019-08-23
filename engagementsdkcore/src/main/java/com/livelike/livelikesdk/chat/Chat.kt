package com.livelike.livelikesdk.chat

import com.livelike.engagementsdkapi.EpochTime

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
    var message: String,
    val senderId: String,
    val senderDisplayName: String,
    var id: String,
    val timeStamp: String = "",
    var isFromMe: Boolean = false
)
