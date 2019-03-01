package com.livelike.livelikesdk.messaging.sendbird

import android.util.Log
import com.google.gson.GsonBuilder
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.util.DateDeserializer
import com.livelike.livelikesdk.util.DateSerializer
import com.sendbird.android.OpenChannel
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class SendbirdChatClient : ChatClient {
    private val TAG = javaClass.simpleName
    private val gson = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, DateDeserializer())
        .registerTypeAdapter(ZonedDateTime::class.java, DateSerializer())
        .create()

    override fun sendMessage(message: ClientMessage) {
        val messageTimestamp = gson.toJson(
            SendbirdMessagingClient.MessageData(
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(message.timeStamp.timeSinceEpochInMs), ZoneId.of("UTC")
                )
            )
        )

        OpenChannel.getChannel(message.channel) { openChannel, exception ->
            openChannel?.sendUserMessage(
                message.message.get("message").asString, messageTimestamp,
                null, null
            ) {
                    userMessage, exception ->
                if (exception != null){
                    Log.e(TAG, "Error sending the message: ")
                }
            }
        }
    }

    override fun updateMessage(message: ClientMessage) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteMessage(message: ClientMessage) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}