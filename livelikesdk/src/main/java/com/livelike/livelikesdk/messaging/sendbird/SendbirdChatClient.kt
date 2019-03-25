package com.livelike.livelikesdk.messaging.sendbird

import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.util.logError
import com.sendbird.android.BaseChannel
import com.sendbird.android.OpenChannel
import com.sendbird.android.UserMessage
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

internal class SendbirdChatClient : ChatClient {
    private val TAG = javaClass.simpleName
    private val zoneUTC = ZoneId.of("UTC")
    override var messageHandler : ChatClientResultHandler? = null

    override fun sendMessage(message: ClientMessage) {
        val messageTimestamp = gson.toJson(
            SendbirdMessagingClient.MessageData(
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(message.timeStamp.timeSinceEpochInMs), zoneUTC
                )
            )
        )
        OpenChannel.getChannel(message.channel) { openChannel, exception ->
            openChannel?.sendUserMessage(
                message.message.get("message").asString,
                messageTimestamp, null, null
            ) { userMessage, e ->
                e?.also { logError { "Error sending the message: $it" } }
            }
        }
    }

    override fun updateMessage(message: ClientMessage) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteMessage(message: ClientMessage) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateMessagesSinceMessage(messageId: String, channel: String) {
        OpenChannel.getChannel(channel) { openChannel, exception ->
            openChannel.getNextMessagesById(messageId.toLong(),
                false,
                SendbirdMessagingClient.CHAT_HISTORY_LIMIT,
                false,
                BaseChannel.MessageTypeFilter.ALL,
                "",
                BaseChannel.GetMessagesHandler { list, e ->
                    if (e != null) {
                        logError { e }
                        return@GetMessagesHandler
                    }
                    messageHandler?.handleMessages( list.map { SendBirdUtils.clientMessageFromBaseMessage(it as UserMessage, openChannel) })
                })
        }
    }
}