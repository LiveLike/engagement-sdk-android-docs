package com.livelike.engagementsdkapi

import android.content.Context

interface ChatEventListener {
    fun onChatMessageSend(message: ChatMessage, timeData: EpochTime)
}

interface ChatRenderer {
    var chatListener: ChatEventListener?
    val chatContext: Context
    fun displayChatMessage(message: ChatMessage)
    fun loadComplete()
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
    val message: String,
    val senderId: String,
    val senderDisplayName: String,
    val id: String,
    val timeStamp: String = ""
)