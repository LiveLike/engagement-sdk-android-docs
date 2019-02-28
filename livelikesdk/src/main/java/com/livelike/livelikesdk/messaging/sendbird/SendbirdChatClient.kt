package com.livelike.livelikesdk.messaging.sendbird

import android.util.Log
import com.livelike.livelikesdk.messaging.ClientMessage
import com.sendbird.android.OpenChannel

class SendbirdChatClient() : ChatClient {
    private val TAG = javaClass.simpleName

    override fun sendMessage(message: ClientMessage) {
        OpenChannel.getChannel(message.channel) { openChannel, exception ->
            openChannel?.sendUserMessage(message.message.get("message").asString,
                message.timeStamp.timeSinceEpochInMs.toString(), null, null) {
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