package com.example.example

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage


data class PinMessageInfo(
    @SerializedName("id")
    var id: String? = null,
    @SerializedName("url")
    var url: String? = null,
    @SerializedName("message_id")
    var messageId: String? = null,
    @SerializedName("message_payload")
    var messagePayload: LiveLikeChatMessage? = null,
    @SerializedName("chat_room_id")
    var chatRoomId: String? = null,
    @SerializedName("pinned_by_id")
    var pinnedById: String? = null

)