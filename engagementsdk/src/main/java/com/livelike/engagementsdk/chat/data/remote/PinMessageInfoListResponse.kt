package com.livelike.engagementsdk.chat.data.remote

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.chat.data.toChatMessage
import com.livelike.engagementsdk.publicapis.toLiveLikeChatMessage

internal data class PinMessageInfoListResponse(
    @field:SerializedName("next")
    val next: String? = null,

    @field:SerializedName("previous")
    val previous: String? = null,

    @field:SerializedName("count")
    val count: Int? = null,

    @field:SerializedName("results")
    val results: List<PubnubPinMessageInfo>? = null
)

internal data class PubnubPinMessageInfo(
    @SerializedName("id")
    var id: String? = null,
    @SerializedName("url")
    var url: String? = null,
    @SerializedName("message_id")
    var messageId: String? = null,
    @SerializedName("message_payload")
    var messagePayload: PubnubChatMessage? = null,
    @SerializedName("chat_room_id")
    var chatRoomId: String? = null,
    @SerializedName("pinned_by_id")
    var pinnedById: String? = null
)


internal fun PubnubPinMessageInfo.toPinMessageInfo(): PinMessageInfo {
    return PinMessageInfo(
        id,
        url,
        messageId,
        messagePayload?.toChatMessage(
            "",
            0L,
            mutableMapOf(),
            null,
            PubnubChatEventType.MESSAGE_CREATED
        )?.toLiveLikeChatMessage(),
        chatRoomId, pinnedById
    )
}