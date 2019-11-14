package com.livelike.engagementsdk.services.messaging.sendbird

import android.content.Context
import com.google.gson.JsonObject
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.ChatViewModel
import com.livelike.engagementsdk.data.repository.UserRepository
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.logDebug
import com.livelike.engagementsdk.utils.logError
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
    private val liveLikeUser: UserRepository,
    private val messageListener: MessageListener
) :
    MessagingClient, ChatClient {
    private val zoneUTC = ZoneId.of("UTC")
    var lastChatMessage: Pair<String, String>? = null

    companion object {
        val CHAT_HISTORY_LIMIT = 50
    }

    private var listener: MessagingEventListener? = null
    private var connectedChannels: MutableList<OpenChannel> = mutableListOf()
    private val messageIdMap = mutableMapOf<String, MutableList<Long>>()

    init {
        liveLikeUser.currentUserStream.subscribe(javaClass) {
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
        SendBird.connect(livelikeUser.id, object : SendBird.ConnectHandler {
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
            })
    }

    private fun MutableMap<String, MutableList<Long>>.addToMap(channel : String, messageId : Long){
        if(this[channel] == null){
            this[channel] = mutableListOf(messageId)
        }else{
            this[channel]?.add(messageId)
        }
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
                analyticsService.trackMessageSent(msg.messageId.toString(), msg.message)
                lastChatMessage = Pair(msg.messageId.toString(), channel)

                messageIdMap.addToMap(openChannel.url, msg.messageId)

                val newMsg = JsonObject().apply {
                    addProperty("event", ChatViewModel.EVENT_MESSAGE_ID_UPDATED)
                    addProperty("new-id", "${msg.messageId}")
                    addProperty("old-id", clientMessage.id)
                }
                listener?.onClientMessageEvent(this@SendbirdMessagingClient, ClientMessage(newMsg, openChannel.url,
                    EpochTime(0)
                ))
            }
        }
    }

    override fun stop() {
        SendBird.disconnect {}
    }

    override fun resume() {
        liveLikeUser.currentUserStream.latest()?.let { connectToSendbird(it, true) }
    }

    data class MessageData(
        val program_date_time: ZonedDateTime
    )

    override fun subscribe(channels: List<String>) {
        channels.forEach { channelUrl ->
            OpenChannel.getChannel(channelUrl,
                OpenChannel.OpenChannelGetHandler { openChannel, e ->
                    if (e != null) { // Error, if the channel doesn't exist.
                        logError { e }
                        if (e.code == 400201) { // Code for channel not found, we will create a new channel
                            createAndJoinChannel(channelUrl)
                        }
                        return@OpenChannelGetHandler
                    }
                    enterChannel(openChannel)
                    loadMessageHistory(openChannel)
                })
        }
    }

    private fun createAndJoinChannel(channelUrl: String) {
        OpenChannel.createChannelWithOperatorUserIds(
            channelUrl,
            channelUrl,
            "",
            "",
            "",
            listOf<String>()
        ) { openChannel: OpenChannel?, sendBirdException: SendBirdException? ->
            if (openChannel == null || sendBirdException != null) {
                return@createChannelWithOperatorUserIds
            }
            enterChannel(openChannel)
            loadMessageHistory(openChannel)
        }
    }

    private fun loadMessageHistory(openChannel: OpenChannel) {
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
                    if (messageIdMap[openChannel.url] == null || !messageIdMap[openChannel.url]!!.contains(message.messageId)) {
                        listener?.onClientMessageEvent(
                            this@SendbirdMessagingClient,
                            SendBirdUtils.clientMessageFromBaseMessage(
                                message as UserMessage,
                                openChannel
                            )
                        )
                        message as UserMessage
                        messageListener.onNewMessage(message.channelUrl, LiveLikeChatMessage(message.sender.nickname, message.message, message.data, message.messageId))
                        messageIdMap.addToMap(openChannel.url, message.messageId)
                    }
                }
                val msg = JsonObject().apply {
                    addProperty("event", ChatViewModel.EVENT_LOADING_COMPLETE)
                }
                listener?.onClientMessageEvent(
                    this@SendbirdMessagingClient, ClientMessage(
                        msg, openChannel.url,
                        EpochTime(0)
                    )
                )
            })
    }

    private fun enterChannel(openChannel: OpenChannel) {
        openChannel.enter(OpenChannel.OpenChannelEnterHandler { exception ->
            if (exception != null) { // Error.
                return@OpenChannelEnterHandler
            }
            connectedChannels.add(openChannel)

            SendBird.addChannelHandler(openChannel.url, object : SendBird.ChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel?, message: BaseMessage?) {
                    if (message != null && channel != null && openChannel.url == message.channelUrl) {
                        message as UserMessage

                        val clientMessage =
                            SendBirdUtils.clientMessageFromBaseMessage(message, channel)
                        if  (messageIdMap[openChannel.url] == null || !messageIdMap[openChannel.url]!!.contains(message.messageId)) {
                            logDebug { "${Date(SendBirdUtils.getTimeMsFromMessageData(message.data))} - Received message from SendBird: $clientMessage" }
                            lastChatMessage = Pair(
                                clientMessage.message.get("id").asString,
                                clientMessage.channel
                            )
                            listener?.onClientMessageEvent(
                                this@SendbirdMessagingClient,
                                clientMessage
                            )

                            messageListener.onNewMessage(message.channelUrl, LiveLikeChatMessage(message.sender.nickname, message.message, message.data, message.messageId))
                            messageIdMap.addToMap(openChannel.url, message.messageId)
                        }
                    }
                }

                override fun onMessageDeleted(channel: BaseChannel?, msgId: Long) {
                    if (channel != null) {
                        val msg = JsonObject().apply {
                            addProperty("event", ChatViewModel.EVENT_MESSAGE_DELETED)
                            addProperty("id", "$msgId")
                        }
                        listener?.onClientMessageEvent(
                            this@SendbirdMessagingClient, ClientMessage(
                                msg, channel.url,
                                EpochTime(0)
                            )
                        )
                    }
                    super.onMessageDeleted(channel, msgId)
                }
            })
        })
    }

    override fun unsubscribe(channels: List<String>) {
        channels.forEach {
            SendBird.removeChannelHandler(it)
            connectedChannels.remove(connectedChannels.find { openChannel -> openChannel.url == it }.apply {
                this?.exit { }
            })
        }
    }

    override fun unsubscribeAll() {
        unsubscribe(connectedChannels.map { it.url })
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
