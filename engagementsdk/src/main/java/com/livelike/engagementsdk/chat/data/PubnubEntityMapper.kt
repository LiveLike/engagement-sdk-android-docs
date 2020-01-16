package com.livelike.engagementsdk.chat.data

import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.ChatMessageReaction
import com.livelike.engagementsdk.chat.data.remote.PubnubChatMessage

internal fun ChatMessage.toPubnubChatMessage(programDateTime: String): PubnubChatMessage {

    return PubnubChatMessage(
        id,
        message,
        senderId,
        senderDisplayPic,
        senderDisplayName,
        programDateTime
    )
}

internal fun PubnubChatMessage.toChatMessage(
    channel: String,
    timetoken: Long,
    emojiCountMap: MutableMap<String, Int>,
    myReaction: ChatMessageReaction?
): ChatMessage {

    return ChatMessage(
        channel,
        message,
        senderId,
        senderNickname,
        senderImageUrl,
        messageId,
        pubnubMessageToken = messageToken,
        timetoken = timetoken,
        emojiCountMap = emojiCountMap,
        myChatMessageReaction = myReaction
    )
}
