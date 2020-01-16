package com.livelike.engagementsdk.chat.data.remote

import com.google.gson.annotations.SerializedName

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
    MESSAGE_DELETED("message-deleted")
}
