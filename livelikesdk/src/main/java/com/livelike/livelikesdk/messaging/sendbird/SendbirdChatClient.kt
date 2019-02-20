package com.livelike.livelikesdk.messaging.sendbird

import android.content.Context
import android.util.Log
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.MessagingEventListener
import com.livelike.livelikesdk.messaging.proxies.SynchronizedMessagingClient
import com.livelike.livelikesdk.messaging.proxies.syncTo
import com.sendbird.android.OpenChannel

class SendbirdChatClient(private val messagingEventListener: MessagingEventListener) : ChatClient {

    private lateinit var messagingClient: SynchronizedMessagingClient
    private val channelUrl = "program_00f4cdfd_6a19_4853_9c21_51aa46d070a0" // TODO: Get this from backend
    private val TAG = javaClass.simpleName

    override fun setSession(session: LiveLikeContentSession, context: Context) {
        messagingClient =
            SendbirdMessagingClient(session.contentSessionId, context).syncTo { session.getPlayheadTime() }
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