package com.livelike.engagementsdk.services.messaging.sendbird

import com.google.gson.JsonObject
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.chat.ChatViewModel
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.logWarn
import com.sendbird.android.BaseChannel
import com.sendbird.android.UserMessage

internal class SendBirdUtils {
    companion object {
        fun clientMessageFromBaseMessage(message: UserMessage, channel: BaseChannel): ClientMessage {
            val messageJson = JsonObject()
            messageJson.addProperty("event", ChatViewModel.EVENT_NEW_MESSAGE)
            messageJson.addProperty("message", message.message)
            messageJson.addProperty("sender", message.sender.nickname)
            messageJson.addProperty("sender_id", message.sender.userId)
            messageJson.addProperty("id", message.messageId)

            val timeMs = getTimeMsFromMessageData(message.data)
            val timeData = EpochTime(timeMs)
            return ClientMessage(messageJson, channel.url, timeData)
        }

        fun getTimeMsFromMessageData(messageDataJson: String): Long {
            try {
                return if (gson.fromJson(messageDataJson, SendbirdMessagingClient.MessageData::class.java) == null) {
                    0
                } else {
                    val messageData = gson.fromJson(messageDataJson, SendbirdMessagingClient.MessageData::class.java)
                    messageData?.program_date_time?.toInstant()?.toEpochMilli() ?: 0 // return the value, or 0 if null
                }
            } catch (e: Exception) {
                // This is here because on some channels historic messages may have Date/Time format is not correct, or Json is off
                logWarn { messageDataJson + "error: " + e }
            }
            return 0
        }
    }
}
