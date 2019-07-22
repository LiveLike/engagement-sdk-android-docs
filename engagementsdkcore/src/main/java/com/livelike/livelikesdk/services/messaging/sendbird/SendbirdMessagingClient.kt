package com.livelike.livelikesdk.services.messaging.sendbird

import android.content.Context
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.livelikesdk.services.analytics.AnalyticsService
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
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.Date

internal class SendbirdMessagingClient(
    private val subscribeKey: String,
    val context: Context,
    private val analyticsService: AnalyticsService,
    private val liveLikeUser: LiveLikeUser?
) :
    MessagingClient, ChatClientResultHandler {
    private val zoneUTC = ZoneId.of("UTC")
    var lastChatMessage: Pair<String, String>? = null

    companion object {
        val CHAT_HISTORY_LIMIT = 50
    }

    private var listener: MessagingEventListener? = null
    private var connectedChannels: MutableList<OpenChannel> = mutableListOf()
    private val userId = fetchUserId()
    private val messageIdList = mutableListOf<Long>()

    init {
        connectToSendbird()
    }

    private fun connectToSendbird() {
        SendBird.init(subscribeKey, context)
        SendBird.connect(userId, object : SendBird.ConnectHandler {
            override fun onConnected(user: User?, e: SendBirdException?) {
                if (e != null || user == null) { // Error.
                    return
                }
                SendBird.updateCurrentUserInfo(fetchUsername(), null,
                    UserInfoUpdateHandler { exception ->
                        if (exception != null) { // Error.
                            return@UserInfoUpdateHandler
                        }
                    })
            }
        })
    }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        val messageTimestamp = gson.toJson(
            MessageData(
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(timeSinceEpoch.timeSinceEpochInMs), zoneUTC
                )
            )
        )
        OpenChannel.getChannel(channel) { openChannel, _ ->
            openChannel?.sendUserMessage(
                message,
                messageTimestamp, null, null
            ) { msg, e ->
                e?.also { logError { "Error sending the message: $it" } }
                analyticsService.trackMessageSent(msg.messageId.toString(), msg.message.length)
                lastChatMessage = Pair(msg.messageId.toString(), channel)
                messageIdList.add(msg.messageId)
            }
        }
    }

    override fun stop() {
        SendBird.disconnect {}
    }

    override fun resume() {
        connectToSendbird()
    }

    private fun fetchUserId(): String {
        return liveLikeUser?.sessionId ?: "empty-id"
    }

    private fun fetchUsername(): String {
        return liveLikeUser?.userName ?: "John Doe"
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

internal interface ChatClientResultHandler {
    fun handleMessages(messages: List<ClientMessage>)
}