package com.livelike.engagementsdk.services.messaging.proxies

import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.utils.Queue
import com.livelike.engagementsdk.utils.logVerbose
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val SYNC_TIME_FIDELITY = 500L

internal class SynchronizedMessagingClient(
    upstream: MessagingClient,
    var timeSource: () -> EpochTime,
    private val validEventBufferMs: Long
) :
    MessagingClientProxy(upstream) {

    private val queueMap:MutableMap<String,Queue<ClientMessage>> = mutableMapOf()
    private var coroutineTimer: Job

    init {
        coroutineTimer = MainScope().launch {
            publishTimeSynchronizedMessageFromQueue()
        }
    }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        upstream.stop()
    }

    override fun start() {
        upstream.start()
    }

    private suspend fun publishTimeSynchronizedMessageFromQueue() {
        processQueueForScheduledEvent()
        delay(SYNC_TIME_FIDELITY)
        publishTimeSynchronizedMessageFromQueue()
    }

    override fun unsubscribeAll() {
        coroutineTimer.cancel()
        super.unsubscribeAll()
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        when {
            shouldPublishEvent(event) -> publishEvent(event)
            shouldDismissEvent(event) -> {
                logDismissedEvent(event)
                return
            }
            else -> {
                // Adding the check for similar message ,it is occuring if user get message
                // from history and also receive message from pubnub listener
                // checking right now is based on id
                val queue = queueMap[event.channel] ?: Queue()
                val currentChatMessageId = event.message.get("id")?.asString
                val foundMsg = queue.elements.find {
                    val msgId = it.message.get("id")?.asString
                    return@find msgId != null && currentChatMessageId != null && msgId == currentChatMessageId
                }
                if (foundMsg == null) {
                    queue.enqueue(event)
                }
                queue.elements.sortBy { it.message.get("pubnubMessageToken").asLong }
                queueMap[event.channel] = queue
            }
        }
    }

    fun processQueueForScheduledEvent() {
        queueMap.keys.forEach {
            val queue = queueMap[it]
            queue?.let {
                val event = queue.peek()
                event?.let {
                    when {
                        shouldPublishEvent(event) -> publishEvent(queue.dequeue()!!)
                        shouldDismissEvent(event) -> {
                            logDismissedEvent(event)
                            queue.dequeue()
                        }
                        else -> {
                        }
                    }
                }
            }
        }

     }

    private fun publishEvent(event: ClientMessage) {
        listener?.onClientMessageEvent(this, event)
    }

    private fun shouldPublishEvent(event: ClientMessage): Boolean {
        val timeStamp = timeSource()
        return timeStamp <= EpochTime(0) || // Timesource return 0 - sync disabled
                event.timeStamp <= EpochTime(0) || // Event time is 0 - bypass sync
                (event.timeStamp <= timeStamp && event.timeStamp >= timeStamp - validEventBufferMs)
    }

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
