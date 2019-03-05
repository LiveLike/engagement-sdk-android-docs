package com.livelike.livelikesdk.messaging.sendbird

import android.content.Context
import com.google.gson.JsonObject
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.MessagingEventListener
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.util.logDebug
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.OpenChannel
import com.sendbird.android.SendBird
import com.sendbird.android.SendBird.UserInfoUpdateHandler
import com.sendbird.android.SendBirdException
import com.sendbird.android.User
import com.sendbird.android.UserMessage
import org.threeten.bp.ZonedDateTime
import java.util.*


class SendbirdMessagingClient (subscribeKey: String, val context: Context) : MessagingClient {

    private var listener : MessagingEventListener? = null
    private val TAG = javaClass.simpleName
    private var connectedChannels : MutableList<OpenChannel> = mutableListOf()

    init {
        val userId = fetchUserId()

        SendBird.init(subscribeKey, context)
        SendBird.connect(userId, object : SendBird.ConnectHandler {
            override fun onConnected(user: User, e: SendBirdException?) {
                if (e != null) {    // Error.
                    return
                }
                SendBird.updateCurrentUserInfo(fetchUsername(), null,
                    UserInfoUpdateHandler { exception ->
                        if (exception != null) {    // Error.
                            return@UserInfoUpdateHandler
                        }
                    })
            }
        })
    }

    private fun fetchUserId() : String {
        // TODO: Get username from backend session + local storage until we allow user to modify their username.
        return "user-idoo"
    }

    private fun fetchUsername() : String {
        // TODO: Get username from backend session + local storage until we allow user to modify their username.
        return "Username-123oo"
    }

    data class MessageData(
        val program_date_time: ZonedDateTime
    )

    override fun subscribe(channels: List<String>) {
        channels.forEach {
            OpenChannel.getChannel(it,
                OpenChannel.OpenChannelGetHandler { openChannel, e ->
                    if (e != null) {    // Error.
                        return@OpenChannelGetHandler
                    }

                    openChannel.enter(OpenChannel.OpenChannelEnterHandler { exception ->
                        if (exception != null) {    // Error.
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
                                        0
                                    }else{
                                        gson.fromJson(message.data, MessageData::class.java).program_date_time
                                            .toInstant().toEpochMilli()
                                    }

                                    val timeData = EpochTime(timeMs)
                                    val clientMessage = ClientMessage(messageJson, channel.url, timeData)
                                    logDebug { "${Date(timeMs)} - Received message from SendBird: $clientMessage" }
                                    listener?.onClientMessageEvent(this@SendbirdMessagingClient, clientMessage)
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
}
