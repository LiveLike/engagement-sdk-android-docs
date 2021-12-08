package com.livelike.engagementsdk.chat.data.remote

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.*

internal data class PubnubChatEvent<T>(
    @SerializedName("event")
    val pubnubChatEventType: String,
    @SerializedName("payload")
    val payload: T,
    @SerializedName("pubnubToken")
    val pubnubToken: Long? = null

)

internal enum class PubnubChatEventType(val key: String) {
    MESSAGE_CREATED("message-created"),
    MESSAGE_DELETED("message-deleted"),
    IMAGE_CREATED("image-created"),
    IMAGE_DELETED("image-deleted"),
    CUSTOM_MESSAGE_CREATED("custom-message-created"),
    CHATROOM_UPDATED("chatroom-updated"),
    CHATROOM_ADDED("chat-room-add"),
    CHATROOM_INVITE("chat-room-invite"),
    BLOCK_PROFILE("block-profile"),
    UNBLOCK_PROFILE("unblock-profile"),
}

internal fun String.toPubnubChatEventType(): PubnubChatEventType? =
    when (this) {
        IMAGE_CREATED.key -> IMAGE_CREATED
        IMAGE_DELETED.key -> IMAGE_DELETED
        MESSAGE_DELETED.key -> MESSAGE_DELETED
        MESSAGE_CREATED.key -> MESSAGE_CREATED
        CUSTOM_MESSAGE_CREATED.key -> CUSTOM_MESSAGE_CREATED
        CHATROOM_UPDATED.key -> CHATROOM_UPDATED
        CHATROOM_ADDED.key -> CHATROOM_ADDED
        CHATROOM_INVITE.key -> CHATROOM_INVITE
        BLOCK_PROFILE.key -> BLOCK_PROFILE
        UNBLOCK_PROFILE.key -> UNBLOCK_PROFILE
        else -> null
    }
