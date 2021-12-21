package com.livelike.engagementsdk.chat.data.remote

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage

data class PinMessageInfoRequest(
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("message_payload")
    val messagePayload: LiveLikeChatMessage,
    @SerializedName("chat_room_id")
    val chatRoomId: String
)