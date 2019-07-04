package com.livelike.livelikesdk.services.messaging.sendbird

import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.utils.logError
import com.sendbird.android.OpenChannel
import com.sendbird.android.UserMessage
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

internal class SendbirdChatClient : ChatClient {
    private val TAG = javaClass.simpleName
    private val zoneUTC = ZoneId.of("UTC")
    override var messageHandler: ChatClientResultHandler? = null

    override fun sendMessage(message: ClientMessage, onSuccess: (msgId: String) -> Unit) {
        val messageTimestamp = gson.toJson(
            SendbirdMessagingClient.MessageData(
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(message.timeStamp.timeSinceEpochInMs), zoneUTC
                )
            )
        )
        OpenChannel.getChannel(message.channel) { openChannel, _ ->
            openChannel?.sendUserMessage(
                message.message.get("message").asString,
                messageTimestamp, null, null
            ) { usrMsg, e ->
                run {
                    e?.also { logError { "Error sending the message: $it" } } ?: onSuccess(usrMsg.messageId.toString())
                }
            }
        }
    }

    override fun updateMessage(message: ClientMessage) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteMessage(message: ClientMessage) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun updateMessagesSinceMessage(messageId: String, channel: String) {
        OpenChannel.getChannel(channel) { openChannel, _ ->
            openChannel.createPreviousMessageListQuery().load(
                SendbirdMessagingClient.CHAT_HISTORY_LIMIT, false
            ) { list, e ->
                if (e != null) {
                    logError { e }
                    return@load
                }
                messageHandler?.handleMessages(list.takeLastWhile { it.messageId.toString() > messageId }.map {
                    SendBirdUtils.clientMessageFromBaseMessage(
                        it as UserMessage,
                        openChannel
                    )
                })
            }
        }
    }
}
