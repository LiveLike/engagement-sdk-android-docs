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
    IMAGE_DELETED("image-deleted")
}

internal fun String.toPubnubChatEventType() : PubnubChatEventType? =
    when(this){
        IMAGE_CREATED.key -> IMAGE_CREATED
        IMAGE_DELETED.key -> IMAGE_DELETED
        MESSAGE_DELETED.key -> MESSAGE_DELETED
        MESSAGE_CREATED.key -> MESSAGE_CREATED
        else -> null
    }
