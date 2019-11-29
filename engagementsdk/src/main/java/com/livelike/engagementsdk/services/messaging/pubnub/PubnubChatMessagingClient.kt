package com.livelike.engagementsdk.services.messaging.pubnub

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.ChatViewModel
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEvent
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.data.remote.PubnubChatMessage
import com.livelike.engagementsdk.chat.data.toChatMessage
import com.livelike.engagementsdk.chat.data.toPubnubChatMessage
import com.livelike.engagementsdk.formatIsoLocal8601
import com.livelike.engagementsdk.parseISODateTime
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.services.messaging.Error
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.services.messaging.sendbird.SendbirdMessagingClient.Companion.CHAT_HISTORY_LIMIT
import com.livelike.engagementsdk.utils.Queue
import com.livelike.engagementsdk.utils.extractStringOrEmpty
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.logDebug
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNOperationType
import com.pubnub.api.enums.PNReconnectionPolicy
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.history.PNHistoryResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class PubnubChatMessagingClient(subscriberKey: String, authKey: String, uuid: String, private val analyticsService: AnalyticsService) : MessagingClient {

    private var connectedChannels: MutableSet<String> = mutableSetOf()

    private val publishQueue  = Queue<Pair<String,PubnubChatEvent<PubnubChatMessage>>>()
    private val coroutineScope = MainScope()
    private var isPublishRunning = false


    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        val clientMessage = gson.fromJson(message, ChatMessage::class.java)
        val pubnubChatEvent = PubnubChatEvent(
            PubnubChatEventType.MESSAGE_CREATED.key, clientMessage.toPubnubChatMessage(
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(timeSinceEpoch.timeSinceEpochInMs),
                    org.threeten.bp.ZoneId.of("UTC")
                ).formatIsoLocal8601()
            )
        )
        publishQueue.enqueue(Pair(channel, pubnubChatEvent))
        if(!isPublishRunning){
            startPublishingFromQueue()
        }
    }


    private fun startPublishingFromQueue() {
        isPublishRunning = true
        coroutineScope.async {
            while (!publishQueue.isEmpty()){
             publishQueue.peek()?.let {messageChannelPair ->
                    if(publishMessageToPubnub(messageChannelPair.second,messageChannelPair.first)){
                        publishQueue.dequeue()
                        delay(100) // ensure messages not more than 5 per second as 100ms is pubnub latency
                    }else{
                        delay(2000) // Linear back-off strategy.
                    }
                }
            }
            isPublishRunning = false
        }

    }

    private suspend fun publishMessageToPubnub(
        pubnubChatEvent: PubnubChatEvent<PubnubChatMessage>,
        channel: String
    ) = suspendCoroutine<Boolean>{
        pubnub.publish()
            .message(
                pubnubChatEvent
            )
            .meta(JsonObject().apply {
                addProperty("sender_id", pubnubChatEvent.payload.senderId)
                addProperty("language", "en-us")
            })
            .channel(channel)
            .async(object : PNCallback<PNPublishResult>() {
                override fun onResponse(result: PNPublishResult?, status: PNStatus?) {
                    logDebug { "pub status code: " + status?.statusCode }
                    if (status?.isError == false) {
                        analyticsService.trackMessageSent(
                            pubnubChatEvent.payload.messageId,
                            pubnubChatEvent.payload.message
                        )
                        logDebug { "pub timetoken: " + result?.timetoken!! }
                        it.resume(true)
                    }else{
                        it.resume(false)
                    }
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
        pubnubConfiguration.authKey = authKey
        pubnubConfiguration.uuid = uuid
        pubnubConfiguration.reconnectionPolicy = PNReconnectionPolicy.EXPONENTIAL
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
                processPubnubChatEvent(message.message.asJsonObject, message.channel, client)
            }

            override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {}

        })
    }

    private fun processPubnubChatEvent(
        jsonObject: JsonObject,
        channel: String,
        client: PubnubChatMessagingClient
    ) {
        val event = jsonObject.extractStringOrEmpty("event")
        if (event == PubnubChatEventType.MESSAGE_CREATED.key || event == PubnubChatEventType.MESSAGE_DELETED.key) {
            val pubnubChatEvent: PubnubChatEvent<PubnubChatMessage> = gson.fromJson(jsonObject,
                object : TypeToken<PubnubChatEvent<PubnubChatMessage>>() {}.type)
            var clientMessage: ClientMessage
            if(event == PubnubChatEventType.MESSAGE_CREATED.key) {
                val pdtString = pubnubChatEvent.payload.programDateTime
                var epochTimeMs = 0L
                pdtString?.parseISODateTime()?.let {
                    epochTimeMs = it.toInstant().toEpochMilli()
                }
                 clientMessage = ClientMessage(
                    gson.toJsonTree(pubnubChatEvent.payload.toChatMessage(channel)).asJsonObject.apply {
                        addProperty("event", ChatViewModel.EVENT_NEW_MESSAGE)
                    },
                    channel,
                    EpochTime(epochTimeMs)
                )
            }else{
                clientMessage = ClientMessage(JsonObject().apply {
                        addProperty("event", ChatViewModel.EVENT_MESSAGE_DELETED)
                        addProperty("id", pubnubChatEvent.payload.messageId)
                    },
                    channel,
                    EpochTime(0)
                )
            }
            logDebug { "Received message on ${Thread.currentThread().name} from pubnub: $clientMessage" }
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
            .start(timestamp)
            .reverse(true)
            .includeTimetoken(true)
            .async(object : PNCallback<PNHistoryResult>() {
                override fun onResponse(result: PNHistoryResult?, status: PNStatus?) {
                    sendLoadingCompletedEvent(channel)
                    if (status?.isError == false && result?.messages?.isEmpty() == false) {
                        result.messages.forEach {
                            processPubnubChatEvent(
                                it.entry.asJsonObject,
                                channel,
                                this@PubnubChatMessagingClient
                            )
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


    private fun sendLoadingCompletedEvent(channel: String) {
        val msg = JsonObject().apply {
            addProperty("event", ChatViewModel.EVENT_LOADING_COMPLETE)
        }
        listener?.onClientMessageEvent(
            this, ClientMessage(
                msg, channel,
                EpochTime(0)
            )
        )
    }


    override fun addMessagingEventListener(listener: MessagingEventListener) {
        // More than one triggerListener?
        this.listener = listener
    }

    override fun destroy() {
        coroutineScope.cancel()
        unsubscribeAll()
        pubnub.destroy()
    }
}
