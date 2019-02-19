package com.livelike.livelikesdk.messaging.sendbird

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.MessagingEventListener
import com.sendbird.android.*
import com.sendbird.android.SendBird.UserInfoUpdateHandler




class SendbirdMessagingClient (contentId: String, val context: Context) : MessagingClient {

    private var listener : MessagingEventListener? = null
    private val TAG = javaClass.simpleName
    private var connectedChannels : MutableList<OpenChannel> = mutableListOf()

    init {
        val subscribeKey = fetchSubKey()
        val userId = fetchUserId()
        SendBird.init(subscribeKey, context)
        SendBird.connect(userId, object : SendBird.ConnectHandler {
            override fun onConnected(user: User, e: SendBirdException?) {
                if (e != null) {    // Error.
                    return
                }
                SendBird.updateCurrentUserInfo(fetchUsername(), null,
                    UserInfoUpdateHandler { e ->
                        if (e != null) {    // Error.
                            return@UserInfoUpdateHandler
                        }
                    })
            }
        })
    }

    private fun fetchUserId() : String {
        // TODO: Get username from backend session + local storage until we allow user to modify their username.
        return "user-id"
    }

    private fun fetchUsername() : String {
        // TODO: Get username from backend session + local storage until we allow user to modify their username.
        return "Username-123"
    }

    private fun fetchSubKey(): String {
        //TODO: Get sendbird sub key from content id when backend Application endpoint is integrated.
        return "E5F2FB80-CC44-4BD2-8D1F-F82917563662"
    }

    override fun subscribe(channels: List<String>) {
        channels.forEach {
            OpenChannel.getChannel(it,
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
                                    messageJson.addProperty("id", message.messageId)

                                    val timeMs: Long = if (message.data.isNullOrEmpty()){
                                        System.currentTimeMillis()
                                    }else{
                                        message.data.toLong()
                                    }

                                    val timeData = EpochTime(timeMs)

                                    listener?.onClientMessageEvent(this@SendbirdMessagingClient, ClientMessage(messageJson, channel.url, timeData))
                                }
                            }
                        })
                    })
                })
        }

    }

    override fun unsubscribe(channels: List<String>) {
        channels.forEach {
            SendBird.removeChannelHandler(it)
            connectedChannels.remove(connectedChannels.find { openChannel -> openChannel.url == it })
        }
    }

    override fun unsubscribeAll() {
        SendBird.removeAllChannelHandlers()
    }

    override fun addMessagingEventListener(listener: MessagingEventListener) {
        this.listener = listener
    }

    override fun sendMessage(message: ClientMessage){
        connectedChannels.find { openChannel -> openChannel.url == message.channel }.also {
            it?.sendUserMessage(message.message.get("message").asString, message.timeStamp.timeSinceEpochInMs.toString(), "timestamp", null) { userMessage, exception ->
                if (exception!=null){
                    Log.e(TAG, "Error sending the message")
                }
            }
        }

    }



}
