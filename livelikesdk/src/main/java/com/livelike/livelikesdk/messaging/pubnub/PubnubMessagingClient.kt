package com.livelike.livelikesdk.messaging.pubnub

import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.ConnectionStatus
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.Error
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.MessagingEventListener
import com.livelike.livelikesdk.util.extractStringOrEmpty
import com.livelike.livelikesdk.util.logVerbose
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNOperationType
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class PubnubMessagingClient(subscriberKey: String) : MessagingClient {
    private val pubnubConfiguration: PNConfiguration = PNConfiguration()
    private var pubnub : PubNub
    private var listener : MessagingEventListener? = null

    init {
        pubnubConfiguration.subscribeKey = subscriberKey
        pubnub = PubNub(pubnubConfiguration)
        val client = this

        //Extract SubscribeCallback?
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
                                //TODO: Handle other relevant categories here
                                //Some other category we have yet to handle
                            }
                        }
                    }

                    else -> {
                        //TODO: handle other Operation Types, or default here
                        //some other Operation Type we are not handling yet.
                    }
                }
            }

            val datePattern =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZZZ")

            override fun message(pubnub: PubNub, message: PNMessageResult) {
                val pdtString = message.message.asJsonObject.getAsJsonObject("payload")
                    .extractStringOrEmpty("program_date_time")
                val epochTimeMs = if (pdtString.isEmpty()) 0 else ZonedDateTime.parse(
                    pdtString,
                    datePattern
                ).toInstant().toEpochMilli()
                logMessage(message)
                val clientMessage = ClientMessage(
                    message.message.asJsonObject,
                    message.channel,
                    EpochTime(epochTimeMs),
                    Duration.parse(
                        message.message.asJsonObject.getAsJsonObject("payload")
                            .extractStringOrEmpty("timeout")
                    ).toMillis()
                )
                listener?.onClientMessageEvent(client, clientMessage)
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

    override fun subscribe(channels : List<String>) {
        pubnub.subscribe().channels(channels).execute()
    }

    override fun unsubscribe(channels: List<String>) {
        pubnub.unsubscribe().channels(channels).execute()
    }

    override fun unsubscribeAll() {
        pubnub.unsubscribeAll()
    }

    override fun addMessagingEventListener(listener: MessagingEventListener) {
        //More than one triggerListener?
        this.listener = listener
    }
}