package com.livelike.engagementsdk.services.messaging.proxies

import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.utils.Queue
import com.livelike.engagementsdk.utils.logVerbose
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class SynchronizedMessagingClient(
    upstream: MessagingClient,
    var timeSource: () -> EpochTime,
    private val validEventBufferMs: Long
) :
    MessagingClientProxy(upstream) {
    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        upstream.stop()
    }

    override fun resume() {
        upstream.resume()
    }

    companion object {
        const val SYNC_TIME_FIDELITY = 100L
    }

    private val queue = Queue<ClientMessage>()
    private var coroutineTimer: Job? = null

    override fun subscribe(channels: List<String>) {
        coroutineTimer = GlobalScope.launch {
            publishTimeSynchronizedMessageFromQueue()
        }
        super.subscribe(channels)
    }

    private suspend fun publishTimeSynchronizedMessageFromQueue() {
        processQueueForScheduledEvent()
        delay(SYNC_TIME_FIDELITY)
        publishTimeSynchronizedMessageFromQueue()
    }

    override fun unsubscribeAll() {
        coroutineTimer?.cancel()
        super.unsubscribeAll()
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        when {
            shouldPublishEvent(event) -> publishEvent(event)
            shouldDismissEvent(event) -> {
                logDismissedEvent(event)
                return
            }
            else -> queue.enqueue(event)
        }
    }

    fun processQueueForScheduledEvent() {
        val event = queue.peek() ?: return
        when {
            shouldPublishEvent(event) -> publishEvent(queue.dequeue()!!)
            shouldDismissEvent(event) -> {
                logDismissedEvent(event)
                queue.dequeue()
            }
        }
    }

    private fun publishEvent(event: ClientMessage) {
        listener?.onClientMessageEvent(this, event)
    }

    private fun shouldPublishEvent(event: ClientMessage): Boolean =
        timeSource() <= EpochTime(0) || // Timesource return 0 - sync disabled
                event.timeStamp <= EpochTime(0) || // Event time is 0 - bypass sync
                (event.timeStamp <= timeSource() && event.timeStamp >= timeSource() - validEventBufferMs)

    private fun shouldDismissEvent(event: ClientMessage): Boolean =
        event.timeStamp > EpochTime(0) &&
                (event.timeStamp < timeSource() - validEventBufferMs)

    private fun logDismissedEvent(event: ClientMessage) =
        logVerbose {
            "Dismissed Client Message: ${event.message} -- the message was too old! eventTime" +
                    event.timeStamp.timeSinceEpochInMs +
                    " : timeSourceTime" + timeSource().timeSinceEpochInMs
        }
}

// Extension for MessagingClient to be synced
internal fun MessagingClient.syncTo(
    timeSource: () -> EpochTime,
    validEventBufferMs: Long = Long.MAX_VALUE
): SynchronizedMessagingClient {
    return SynchronizedMessagingClient(this, timeSource, validEventBufferMs)
}
