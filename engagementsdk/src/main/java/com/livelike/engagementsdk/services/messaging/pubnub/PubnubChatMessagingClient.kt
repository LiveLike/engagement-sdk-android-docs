package com.livelike.engagementsdk.services.messaging.pubnub

import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEvent
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.data.remote.PubnubChatMessage
import com.livelike.engagementsdk.parseISODateTime
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.services.messaging.Error
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.services.messaging.sendbird.SendbirdMessagingClient.Companion.CHAT_HISTORY_LIMIT
import com.livelike.engagementsdk.utils.extractStringOrEmpty
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.logDebug
import com.livelike.engagementsdk.utils.logVerbose
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNOperationType
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.history.PNHistoryResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import java.util.Calendar

internal class PubnubChatMessagingClient(subscriberKey: String, uuid: String, private val analyticsService: AnalyticsService) : MessagingClient {

    private var connectedChannels: MutableSet<String> = mutableSetOf()

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        val clientMessage = gson.fromJson(message, ChatMessage::class.java)
        pubnub.publish()
            .message(clientMessage)
            .meta(JsonObject().apply {
                addProperty("sender_id", clientMessage.senderId)
                addProperty("language", "en-us")
            })
            .channel(channel)
            .async(object : PNCallback<PNPublishResult>() {
                override fun onResponse(result: PNPublishResult, status: PNStatus) {
                    // handle publish result, status always present, result if successful
                    // status.isError() to see if error happened
                    if (!status.isError) {
                        analyticsService.trackMessageSent(clientMessage.id, clientMessage.message)
                        println("pub timetoken: " + result.timetoken!!)
                    }
                    println("pub status code: " + status.statusCode)
                }
            })
    }

    override fun stop() {
        pubnub.disconnect()
    }

    override fun start() {
        pubnub.reconnect()
        connectedChannels.forEach { loadMessageHistoryByTimestamp(channel = it) }
    }

    private val pubnubConfiguration: PNConfiguration = PNConfiguration()
    var pubnub: PubNub
    private var listener: MessagingEventListener? = null

    init {
        pubnubConfiguration.subscribeKey = subscriberKey
        pubnubConfiguration.uuid = uuid
        pubnub = PubNub(pubnubConfiguration)
        val client = this

        // Extract SubscribeCallback?
        pubnub.addListener(object : SubscribeCallback() {
            override fun status(pubnub: PubNub, status: PNStatus) {
                when (status.operation) {
                    // let's combine unsubscribe and subscribe handling for ease of use
                    PNOperationType.PNSubscribeOperation, PNOperationType.PNUnsubscribeOperation -> {
                        // note: subscribe statuses never have traditional
                        // errors, they just have categories to represent the
                        // different issues or successes that occur as part of subscribe
                        when (status.category) {
                            PNStatusCategory.PNConnectedCategory -> {
                                // this is expected for a subscribe, this means there is no error or issue whatsoever
                                listener?.onClientMessageStatus(client, ConnectionStatus.CONNECTED)
                            }

                            PNStatusCategory.PNReconnectedCategory -> {
                                // this usually occurs if subscribe temporarily fails but reconnects. This means
                                // there was an error but there is no longer any issue
                                listener?.onClientMessageStatus(client, ConnectionStatus.CONNECTED)
                            }

                            PNStatusCategory.PNDisconnectedCategory -> {
                                // this is the expected category for an unsubscribe. This means there
                                // was no error in unsubscribing from everything
                                listener?.onClientMessageStatus(client, ConnectionStatus.DISCONNECTED)
                            }

                            PNStatusCategory.PNAccessDeniedCategory -> {
                                // this means that PAM does allow this client to subscribe to this
                                // channel and channel group configuration. This is another explicit error
                                listener?.onClientMessageError(client, Error("Access Denied", "Access Denied"))
                            }

                            PNStatusCategory.PNTimeoutCategory, PNStatusCategory.PNNetworkIssuesCategory, PNStatusCategory.PNUnexpectedDisconnectCategory -> {
                                // this is usually an issue with the internet connection, this is an error, handle appropriately
                                pubnub.reconnect()
                            }

                            else -> {
                                // Some other category we have yet to handle
                            }
                        }
                    }

                    else -> {
                        // some other Operation Type we are not handling yet.
                    }
                }
            }

            override fun message(pubnub: PubNub, message: PNMessageResult) {
                logMessage(message)
                processPubnubChatEvent(message.message.asJsonObject, message.channel, client)
            }

            override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {}

            fun logMessage(message: PNMessageResult) {
                logVerbose { "Message publisher: " + message.publisher }
                logVerbose { "Message Payload: " + message.message }
                logVerbose { "Message Subscription: " + message.subscription }
                logVerbose { "Message Channel: " + message.channel }
                logVerbose { "Message timetoken: " + message.timetoken!! }
            }
        })
    }

    private fun processPubnubChatEvent(
        jsonObject: JsonObject,
        channel: String,
        client: PubnubChatMessagingClient
    ) {
        val event = jsonObject.extractStringOrEmpty("event")
        if (event == PubnubChatEventType.MESSAGE_CREATED.key) {
            val pubnubChatEvent: PubnubChatEvent<PubnubChatMessage> = gson.fromJson(jsonObject,
                object : TypeToken<PubnubChatEvent<PubnubChatMessage>>() {}.type)

            val pdtString = pubnubChatEvent.payload.programDateTime
            var epochTimeMs = 0L
            pdtString.parseISODateTime()?.let {
                epochTimeMs = it.toInstant().toEpochMilli()
            }
            val clientMessage = ClientMessage(
                jsonObject,
                channel,
                EpochTime(epochTimeMs)
            )
            logDebug { "$pdtString - Received message from pubnub: $clientMessage" }
            listener?.onClientMessageEvent(client, clientMessage)
        }
    }

    private fun loadMessageHistoryByTimestamp(
        channel: String,
        timestamp: Long = Calendar.getInstance().timeInMillis,
        chatHistoyLimit: Int = CHAT_HISTORY_LIMIT
    ) {
        pubnub.history()
            .channel(channel)
            .count(chatHistoyLimit)
            .end(timestamp)
            .includeTimetoken(true)
            .async(object : PNCallback<PNHistoryResult>() {
                override fun onResponse(result: PNHistoryResult?, status: PNStatus?) {
                    result?.let {
                        result.messages.reversed().forEach {
                            processPubnubChatEvent(it.entry.asJsonObject, channel, this@PubnubChatMessagingClient)
                        }
                    }
                }
            })
    }

    override fun subscribe(channels: List<String>) {
        pubnub.subscribe().channels(channels).execute()
        channels.forEach {
            connectedChannels.add(it)
            loadMessageHistoryByTimestamp(channel = it)
        }
    }

    override fun unsubscribe(channels: List<String>) {
        pubnub.unsubscribe().channels(channels).execute()
        channels.forEach {
            connectedChannels.remove(it)
        }
    }

    override fun unsubscribeAll() {
        pubnub.unsubscribeAll()
    }

    override fun addMessagingEventListener(listener: MessagingEventListener) {
        // More than one triggerListener?
        this.listener = listener
    }

    override fun destroy() {
        unsubscribeAll()
        pubnub.destroy()
    }
}
