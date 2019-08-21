package com.livelike.livelikesdk.services.messaging.sendbird

import android.content.Context
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.livelikesdk.Stream
import com.livelike.livelikesdk.chat.ChatMessage
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.services.messaging.MessagingEventListener
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.utils.logDebug
import com.livelike.livelikesdk.utils.logError
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.OpenChannel
import com.sendbird.android.PreviousMessageListQuery
import com.sendbird.android.SendBird
import com.sendbird.android.SendBird.UserInfoUpdateHandler
import com.sendbird.android.SendBirdException
import com.sendbird.android.User
import com.sendbird.android.UserMessage
import java.util.Date
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

internal class SendbirdMessagingClient(
    private val subscribeKey: String,
    val context: Context,
    private val analyticsService: AnalyticsService,
    liveLikeUser: Stream<LiveLikeUser>
) :
    MessagingClient, ChatClient {
    private val zoneUTC = ZoneId.of("UTC")
    var lastChatMessage: Pair<String, String>? = null

    companion object {
        val CHAT_HISTORY_LIMIT = 50
    }

    private var listener: MessagingEventListener? = null
    private var connectedChannels: MutableList<OpenChannel> = mutableListOf()
    private val messageIdList = mutableListOf<Long>()
    private var user: LiveLikeUser? = null

    init {
        liveLikeUser.subscribe(javaClass) {
            user = it
            it?.let { u ->
                when (SendBird.getConnectionState()) {
                    SendBird.ConnectionState.CLOSED -> connectToSendbird(u)
                    SendBird.ConnectionState.OPEN -> updateNickname(u.nickname) {}
                    else -> {}
                }
            }
        }
    }

    private fun connectToSendbird(livelikeUser: LiveLikeUser, resubscribe: Boolean = false) {
        SendBird.init(subscribeKey, context)
        SendBird.connect(livelikeUser.sessionId, object : SendBird.ConnectHandler {
            override fun onConnected(user: User?, e: SendBirdException?) {
                if (e != null || user == null) { // Error.
                    return
                }
                updateNickname(livelikeUser.nickname) {
                    if (resubscribe) {
                        subscribe(connectedChannels.map { it.url })
                    }
                }
            }
        })
    }

    private fun updateNickname(nickname: String, callback: () -> Unit) {
        SendBird.updateCurrentUserInfo(nickname, null,
            UserInfoUpdateHandler { exception ->
                if (exception != null) { // Error.
                    return@UserInfoUpdateHandler
                }
                callback()
                // TODO: Should be also updated on user profile
            })
    }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        val clientMessage = gson.fromJson(message, ChatMessage::class.java)
        val messageTimestamp = gson.toJson(
            MessageData(
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(timeSinceEpoch.timeSinceEpochInMs), zoneUTC
                )
            )
        )
        OpenChannel.getChannel(channel) { openChannel, _ ->
            openChannel?.sendUserMessage(
                clientMessage.message,
                messageTimestamp, null, null
            ) { msg, e ->
                e?.also {
                    logError { "Error sending the message: ${it.stackTrace}" }
                }
                analyticsService.trackMessageSent(msg.messageId.toString(), msg.message.length)
                lastChatMessage = Pair(msg.messageId.toString(), channel)
                messageIdList.add(msg.messageId)

                val newMsg = JsonObject().apply {
                    addProperty("event", "id-updated")
                    addProperty("new-id", "${msg.messageId}")
                    addProperty("old-id", clientMessage.id)
                }
                listener?.onClientMessageEvent(this@SendbirdMessagingClient, ClientMessage(newMsg, openChannel.url, EpochTime(0)))
            }
        }
    }

    override fun stop() {
        SendBird.disconnect {}
    }

    override fun resume() {
        user?.let { connectToSendbird(it, true) }
    }

    data class MessageData(
        val program_date_time: ZonedDateTime
    )

    override fun subscribe(channels: List<String>) {
        channels.forEach {
            OpenChannel.getChannel(it,
                OpenChannel.OpenChannelGetHandler { openChannel, e ->
                    if (e != null) { // Error.
                        return@OpenChannelGetHandler
                    }

                    openChannel.enter(OpenChannel.OpenChannelEnterHandler { exception ->
                        if (exception != null) { // Error.
                            return@OpenChannelEnterHandler
                        }
                        connectedChannels.add(openChannel)

                        SendBird.addChannelHandler(openChannel.url, object : SendBird.ChannelHandler() {
                            override fun onMessageReceived(channel: BaseChannel?, message: BaseMessage?) {
                                if (message != null && channel != null && openChannel.url == message.channelUrl) {
                                    message as UserMessage
                                    val clientMessage = SendBirdUtils.clientMessageFromBaseMessage(message, channel)
                                    if (!messageIdList.contains(message.messageId)) {
                                        logDebug { "${Date(SendBirdUtils.getTimeMsFromMessageData(message.data))} - Received message from SendBird: $clientMessage" }
                                        lastChatMessage = Pair(clientMessage.message.get("id").asString, clientMessage.channel)
                                        listener?.onClientMessageEvent(this@SendbirdMessagingClient, clientMessage)
                                        messageIdList.add(message.messageId)
                                    }
                                }
                            }

                            override fun onMessageDeleted(channel: BaseChannel?, msgId: Long) {
                                if (channel != null) {
                                    val msg = JsonObject().apply {
                                        addProperty("event", "deletion")
                                        addProperty("id", "$msgId")
                                    }
                                    listener?.onClientMessageEvent(this@SendbirdMessagingClient, ClientMessage(msg, channel.url, EpochTime(0)))
                                }
                                super.onMessageDeleted(channel, msgId)
                            }
                        })
                    })

                    val prevMessageListQuery = openChannel.createPreviousMessageListQuery()
                    prevMessageListQuery.load(
                        CHAT_HISTORY_LIMIT,
                        true,
                        PreviousMessageListQuery.MessageListQueryResult { messages, err ->
                            if (err != null) {
                                logError { err }
                                return@MessageListQueryResult
                            }
                            for (message: BaseMessage in messages.reversed()) {
                                if (!messageIdList.contains(message.messageId)) {
                                    listener?.onClientMessageEvent(
                                        this@SendbirdMessagingClient,
                                        SendBirdUtils.clientMessageFromBaseMessage(message as UserMessage, openChannel)
                                    )
                                    messageIdList.add(message.messageId)
                                }
                            }
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
        connectedChannels.clear()
    }

    override fun addMessagingEventListener(listener: MessagingEventListener) {
        this.listener = listener
    }

    override fun handleMessages(messages: List<ClientMessage>) {
        messages.forEach {
            listener?.onClientMessageEvent(this, it)
        }
    }
}

internal interface ChatClient {
    fun handleMessages(messages: List<ClientMessage>)
}
