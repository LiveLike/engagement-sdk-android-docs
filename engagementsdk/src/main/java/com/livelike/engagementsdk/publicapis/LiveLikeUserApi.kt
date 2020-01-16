package com.livelike.engagementsdk.publicapis

import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.data.remote.PubnubChatMessage

/**
 * User pojo to be exposed, should be minimal in terms of fields
 */
data class LiveLikeUserApi(
    var nickname: String,
    val accessToken: String
)

// this model is not changed since 1.2 release in hurry, we need to fix it may require to bump to major version.
data class LiveLikeChatMessage(val nickname: String = "", val userPic: String?, val message: String = "", val timestamp: String = "", val id: Long = 0)

internal fun PubnubChatMessage.toLiveLikeChatMessage(): LiveLikeChatMessage {
    // TODO will require to bump to major version as id needs to be string
    return LiveLikeChatMessage(senderNickname, senderImageUrl, message, "", messageId.hashCode().toLong())
}

internal fun ChatMessage.toLiveLikeChatMessage(): LiveLikeChatMessage {
    return LiveLikeChatMessage(senderDisplayName, senderDisplayPic, message, "", id.hashCode().toLong())
}
