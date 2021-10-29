package com.livelike.engagementsdk.publicapis

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.chat.ChatRoomInfo

data class ChatRoomInvitation(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("created_at") val created_at: String,
    @SerializedName("status") val status: String,
    @SerializedName("invited_profile") val invited_profile: LiveLikeUserApi,
    @SerializedName("chat_room") val chat_room: ChatRoomInfo,
    @SerializedName("invited_by") val invited_by: LiveLikeUserApi,
    @SerializedName("chat_room_id") val chat_room_id: String,
    @SerializedName("invited_profile_id") val invited_profile_id: String
)


internal data class ChatRoomInvitationResponse(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<ChatRoomInvitation>
)

enum class ChatRoomInvitationStatus(val key: String) {
    PENDING("pending"), ACCEPTED("accepted"), REJECTED("rejected")
}