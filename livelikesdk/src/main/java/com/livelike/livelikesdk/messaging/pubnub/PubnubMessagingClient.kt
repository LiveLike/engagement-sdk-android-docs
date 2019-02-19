package com.livelike.livelikesdk.messaging.pubnub

import com.livelike.livelikesdk.messaging.*
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNOperationType
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult


class PubnubMessagingClient (val contentId: String) : MessagingClient {
    private val pubnubConfiguration: PNConfiguration = PNConfiguration()
    private var pubnub : PubNub
    private var listener : MessagingEventListener? = null

    init {
        pubnubConfiguration.subscribeKey = fetchSubKey(contentId)
        pubnub = PubNub(pubnubConfiguration)
        val client = this;

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

            override fun message(pubnub: PubNub, message: PNMessageResult) {
                logMessage(message)
                val clientMessage = ClientMessage(
                        message.message.asJsonObject,
                        message.channel,
                        EpochTime( message.timetoken))
                listener?.onClientMessageEvent(client, clientMessage)
            }

            override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {}

            fun logMessage(message: PNMessageResult) {
                println("Message publisher: " + message.publisher)
                println("Message Payload: " + message.message)
                println("Message Subscription: " + message.subscription)
                println("Message Channel: " + message.channel)
                println("Message timetoken: " + message.timetoken!!)
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

    private fun fetchSubKey(contentId: String) : String {
        //TODO: Mechanism for getting pubnub sub key from content id
        return "sub-c-016db434-d156-11e8-b5de-7a9ddb77e130"
    }
}