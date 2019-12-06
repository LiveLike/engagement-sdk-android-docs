package com.livelike.engagementsdk.chat.data.remote

import com.google.gson.annotations.SerializedName

internal data class PubnubChatMessage(

    @SerializedName("id")
    val messageId: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("sender_image_url")
    val senderImageUrl: String?,
    @SerializedName("sender_nickname")
    val senderNickname: String,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("program_date_time")
    val programDateTime: String?
)
