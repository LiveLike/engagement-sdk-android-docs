package com.livelike.livelikesdk.messaging.sendbird

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.MessagingEventListener
import com.sendbird.android.*


class SendbirdMessagingClient (contentId: String, val context: Context) : MessagingClient {

    private var listener : MessagingEventListener? = null
    private val TAG = javaClass.simpleName
    private var connectedChannels : MutableList<OpenChannel> = mutableListOf()

    init {
        val subscribeKey = fetchSubKey(contentId)
        val userId = fetchUserId()
        SendBird.init(subscribeKey, context)
        SendBird.connect(userId, object : SendBird.ConnectHandler {
            override fun onConnected(user: User, e: SendBirdException?) {
                if (e != null) {    // Error.
                    return
                }
            }
        })
    }

    private fun fetchUserId() : String {
        // TODO: Get username from backend session + local storage until we allow user to modify their username.
        return "user-id"
    }

    private fun fetchSubKey(contentId: String): String {
        //TODO: Get sendbird sub key from content id when backend Application endpoint is integrated.
        return "E5F2FB80-CC44-4BD2-8D1F-F82917563662"
    }

    override fun subscribe(channels: List<String>) {
        // TODO: subscribe to all channels
        OpenChannel.getChannel(channels.first(),
            OpenChannel.OpenChannelGetHandler { openChannel, e ->
            if (e != null) {    // Error.
                return@OpenChannelGetHandler
            }

            openChannel.enter(OpenChannel.OpenChannelEnterHandler { e ->
                if (e != null) {    // Error.
                    return@OpenChannelEnterHandler
                }
                connectedChannels.add(openChannel)
                SendBird.addChannelHandler(openChannel.url, object: SendBird.ChannelHandler(){
                    override fun onMessageReceived(channel: BaseChannel?, message: BaseMessage?) {
                        if(message!=null && channel!=null){
                            message as UserMessage
                            val messageJson = JsonObject()
                            messageJson.addProperty("message", message.message)
                            messageJson.addProperty("sender", message.sender.nickname)
                            messageJson.addProperty("sender_id", message.sender.userId)

                            val timeData = EpochTime(System.currentTimeMillis()) // message.data..timestamp TODO: Parse the data to get the message timestamp

                            listener?.onClientMessageEvent(this@SendbirdMessagingClient, ClientMessage(messageJson, channel.url, timeData))
                        }
                    }
                })
            })
        })
    }

    override fun unsubscribe(channels: List<String>) {
        // Unsubscribe from the channel
        connectedChannels.remove(connectedChannels.find { openChannel -> openChannel.url == channels.first() }) // remove the found channels

        if (connectedChannels.isEmpty()){
            // Disconnect from Sendbird if no subscribed channels
            SendBird.disconnect {
                // You are disconnected from SendBird.
            }
        }
    }

    override fun unsubscribeAll() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addMessagingEventListener(listener: MessagingEventListener) {
        this.listener = listener
    }

    override fun sendMessage(message: ClientMessage){
        connectedChannels.find { openChannel -> openChannel.url == message.channel }.also {
            // Send user message
            it?.sendUserMessage(message.message.get("message").asString) { userMessage, exception ->
                if (exception!=null){
                    Log.e(TAG, "Error sending the message")
                }
            }
        }

    }



}
