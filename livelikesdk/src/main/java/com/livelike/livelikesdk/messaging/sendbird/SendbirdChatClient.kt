package com.livelike.livelikesdk.messaging.sendbird

import android.content.Context
import android.os.Handler
import android.util.Log
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.MessagingEventListener
import com.livelike.livelikesdk.messaging.proxies.syncTo
import com.sendbird.android.OpenChannel

class SendbirdChatClient(contentId: String, context: Context, messagingEventListener: MessagingEventListener) : ChatClient{
    private val messagingClient = SendbirdMessagingClient("a_content_id", context).syncTo(EpochTime(System.currentTimeMillis()))
    private val channelUrl = "program_00f4cdfd_6a19_4853_9c21_51aa46d070a0" // TODO: Get this from backend
    private val TAG = javaClass.simpleName

    // sync loop
    private val runnableCode = object: Runnable {
        override fun run() {
            messagingClient.updateTimeSource(EpochTime(System.currentTimeMillis()))
            Handler().postDelayed(this, 1000)
        }
    }

    init {
        runnableCode.run()
        messagingClient.subscribe(listOf(channelUrl))
        messagingClient.addMessagingEventListener(messagingEventListener)
    }

    override fun sendMessage(message: ClientMessage) {
        OpenChannel.getChannel(message.channel) { openChannel, exception ->
            openChannel?.sendUserMessage(message.message.get("message").asString, message.timeStamp.timeSinceEpochInMs.toString(), null, null) { userMessage, exception ->
                if (exception!=null){
                    Log.e(TAG, "Error sending the message")
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