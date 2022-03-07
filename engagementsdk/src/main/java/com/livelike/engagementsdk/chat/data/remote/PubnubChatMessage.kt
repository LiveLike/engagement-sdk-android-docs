package com.livelike.engagementsdk.chat.data.remote

import com.google.gson.annotations.SerializedName

internal data class PubnubChatMessage(

    @SerializedName("id")
    val messageId: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("sender_image_url")
    val senderImageUrl: String?,
    @SerializedName("sender_nickname")
    val senderNickname: String,
    @SerializedName("program_date_time")
    val programDateTime: String?,
    @SerializedName("messageToken")
    val messageToken: Long? = null,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("image_width")
    val image_width: Int?,
    @SerializedName("image_height")
    val image_height: Int?,
    @SerializedName("custom_data")
    val custom_data: String?,
    @SerializedName("quote_message")
    val quoteMessage: PubnubChatMessage? = null,
    @SerializedName("quote_message_id")
    val quoteMessageId: String? = null,
    @SerializedName("client_message_id")
    val clientMessageId: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("message_event")
    var messageEvent: String? = null,
    @SerializedName("chat_room_id")
    var chatRoomId: String? = null,
    @SerializedName("pubnub_timetoken")
    var pubnubTimeToken: Long? = null,
    @SerializedName("reactions")
    var reactions: Map<String, List<PubnubChatReaction>>? = null,
)

internal data class PubnubChatReaction(
    val uuid: String? = null,
    val actionTimeToken: Long? = null
)
