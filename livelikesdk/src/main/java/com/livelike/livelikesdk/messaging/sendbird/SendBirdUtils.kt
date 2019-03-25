package com.livelike.livelikesdk.messaging.sendbird

import com.google.gson.JsonObject
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.util.logError
import com.sendbird.android.BaseChannel
import com.sendbird.android.UserMessage

class SendBirdUtils {
    companion object {
        fun clientMessageFromBaseMessage(message: UserMessage, channel: BaseChannel): ClientMessage {
            val messageJson = JsonObject()
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
                //This is here because on some channels historic messages may have Date/Time format is not correct, or Json is off
                logError { e }
            }
            return 0
        }
    }
}