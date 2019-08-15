package com.livelike.livelikesdk.services.messaging.proxies

import com.livelike.engagementsdkapi.EpochTime
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.MessagingClient
import java.util.PriorityQueue
import java.util.Queue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class DebounceQueueMessagingClient(
    upstream: MessagingClient,
    delayMs: Long
) :
    MessagingClientProxy(upstream) {

    data class MessageHolder(val messagingClient: MessagingClient, val clientMessage: ClientMessage) : Comparable<MessageHolder> {
        override fun compareTo(other: MessageHolder): Int {
            return this.clientMessage.message.get("event").asString.length - other.clientMessage.message.get("event").asString.length
        }
    }

    private val messageQueue: Queue<MessageHolder> = PriorityQueue()

    init {
        GlobalScope.launch {
            while (true) {
                delay(delayMs)
                if (!messageQueue.isNullOrEmpty()) {
                    messageQueue.remove()?.apply {
                        listener?.onClientMessageEvent(messagingClient, clientMessage)
                    }
                }
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

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        messageQueue.add(MessageHolder(client, event))
    }
}

internal fun MessagingClient.debounce(delayMs: Long): DebounceQueueMessagingClient {
    return DebounceQueueMessagingClient(this, delayMs)
}
