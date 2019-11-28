package com.livelike.engagementsdk.chat.data

import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.data.remote.PubnubChatMessage

internal fun ChatMessage.toPubnubChatMessage(programDateTime: String): PubnubChatMessage {

    return PubnubChatMessage(
        id,
        message,
        senderId,
        senderDisplayPic,
        senderDisplayName,
        timeStamp,
        programDateTime
    )
}

internal fun PubnubChatMessage.toChatMessage(channel: String): ChatMessage {

    return ChatMessage(
        channel,
        message,
        senderId,
        senderNickname,
        senderImageUrl,
        messageId,
        createdAt
    )
}
