package com.livelike.engagementsdk.services.messaging.pubnub

import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.parseISODateTime
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.proxies.MessagingClientProxy
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.utils.extractStringOrEmpty
import com.livelike.engagementsdk.utils.logDebug
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.history.PNHistoryResult
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Can Replay messages by the no of counts provided if available in history, u can think of it as a Rx replay subject.
 * Need to refactor messaging client(its 2 way currently), Probably by terminology it is ok client can flow in 2 ways.
 * Need to add loggings here, refactor parsing logic(Should be high priority).
 */
internal class PubnubMessagingClientReplay(
    upstream: PubnubMessagingClient,
    var count: Int
) : MessagingClientProxy(upstream) {

    init {
        count = count.coerceAtMost(100)
    }

    private var isConnected: Boolean = false
    private var pubnub: PubNub = upstream.pubnub

    private var channelLastMessageMap = mutableMapOf<String, ClientMessage>()
    private val pendingChannelsForAddingReplay = CopyOnWriteArrayList<String>()

    override fun subscribe(channels: List<String>) {
        super.subscribe(channels)
        if (isConnected) {
            channels.forEach { channel ->
                if (!channelLastMessageMap.containsKey(channel)) {
                    fetchLastMessageFromHistoryToReplay(channel)
                } else {
                    channelLastMessageMap[channel]?.let {
                        listener?.onClientMessageEvent(this, it)
                    }
                }
            }
        } else {
            pendingChannelsForAddingReplay.addAll(channels)
        }
    }

    private fun fetchLastMessageFromHistoryToReplay(channel: String) {
        pubnub.history()
            .channel(channel)
            .count(count)
            .end(0)
            .includeTimetoken(true)
            .async(object : PNCallback<PNHistoryResult>() {
                override fun onResponse(result: PNHistoryResult?, status: PNStatus?) {
                        result?.let {
                            result.messages.reversed().forEach {
                                val payload = it.entry.asJsonObject.getAsJsonObject("payload")
                                val timeoutReceived = payload.extractStringOrEmpty("timeout")
                                val pdtString = payload.extractStringOrEmpty("program_date_time")
                                var epochTimeMs = 0L
                                pdtString.parseISODateTime()?.let {
                                    epochTimeMs = it.toInstant().toEpochMilli()
                                }
                                val timeoutMs = AndroidResource.parseDuration(timeoutReceived)

                                val clientMessage = ClientMessage(
                                    it.entry.asJsonObject,
                                    channel,
                                    EpochTime(epochTimeMs),
                                    timeoutMs
                                )
                                logDebug { "$pdtString - Received history message from pubnub: $clientMessage" }
                                listener?.onClientMessageEvent(this@PubnubMessagingClientReplay, clientMessage)
                            }
                        }
                }
            })
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        super.onClientMessageEvent(client, event)
        channelLastMessageMap[event.channel] = event
    }

    override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {
        super.onClientMessageStatus(client, status)
        isConnected = status == ConnectionStatus.CONNECTED
        if (isConnected) {
            val iterator = pendingChannelsForAddingReplay.iterator()
            for (i in iterator) {
                fetchLastMessageFromHistoryToReplay(i)
                iterator.remove()
            }
        }
    }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        upstream.stop()
    }

    override fun resume() {
        upstream.resume()
    }
}

// Think of it as adding a behaviouralSubject functionalities to the pubnub channels
internal fun PubnubMessagingClient.asBehaviourSubject(): PubnubMessagingClientReplay {
    return PubnubMessagingClientReplay(this, 1)
}
