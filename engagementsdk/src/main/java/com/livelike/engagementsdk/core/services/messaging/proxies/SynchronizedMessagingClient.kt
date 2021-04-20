package com.livelike.engagementsdk.core.services.messaging.proxies

import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logVerbose
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.PriorityQueue

const val SYNC_TIME_FIDELITY = 500L

internal class SynchronizedMessagingClient(
    upstream: MessagingClient,
    var timeSource: () -> EpochTime,
    private val validEventBufferMs: Long
) :
    MessagingClientProxy(upstream) {

    private val queueMap: MutableMap<String, PriorityQueue<ClientMessage>> = mutableMapOf()
    private var publishSyncMessagesJob: Job
    private var isQueueProcess: Boolean = false

    private val messageComparator : Comparator<ClientMessage> =
        Comparator<ClientMessage> { o1, o2 ->
            (o1.timeStamp.timeSinceEpochInMs - o2.timeStamp.timeSinceEpochInMs).toInt()
        }

    init {
        publishSyncMessagesJob = MainScope().launch {
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

    override fun onClientMessageEvents(client: MessagingClient, events: List<ClientMessage>) {

        val list = events.filter { event ->
            when {
                shouldPublishEvent(event) -> {
                    return@filter true
                }
                shouldDismissEvent(event) -> {
                    logDismissedEvent(event)
                    return@filter false
                }
                else -> {
                    addMessageToQueue(event)
                    return@filter false
                }
            }
        }
        listener?.onClientMessageEvents(this, list.sortedWith(messageComparator))
        processQueueForScheduledEvent()
    }

    private suspend fun publishTimeSynchronizedMessageFromQueue() {
        processQueueForScheduledEvent()
        delay(SYNC_TIME_FIDELITY)
        publishTimeSynchronizedMessageFromQueue()
    }

    override fun unsubscribeAll() {
        publishSyncMessagesJob.cancel()
        super.unsubscribeAll()
    }

    private val DEFAULT_QUEUE_CAPACITY = 20

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        logDebug { "Message received at SynchronizedMessagingClient" }
        when {
            shouldPublishEvent(event) -> {
                val queue = queueMap[event.channel] ?: PriorityQueue(
                    DEFAULT_QUEUE_CAPACITY,
                    messageComparator
                )
                if (queue.isEmpty().not()) {
                    processQueueForScheduledEvent()
                }
                publishEvent(event)
            }
            shouldDismissEvent(event) -> {
                logDismissedEvent(event)
                return
            }
            else -> {
                // Adding the check for similar message ,it is occuring if user get message
                // from history and also receive message from pubnub listener
                // checking right now is based on id
                addMessageToQueue(event)
            }
        }
    }

    private fun addMessageToQueue(event: ClientMessage) {
        // Adding the check for similar message ,it is occuring if user get message
        // from history and also receive message from pubnub listener
        // checking right now is based on id
        val queue = queueMap[event.channel] ?: PriorityQueue(
            DEFAULT_QUEUE_CAPACITY,
            messageComparator
        )
        val currentChatMessageId = event.message.get("id")?.asString

        val foundMsg = queue.find {
            val msgId = it.message.get("id")?.asString
            return@find msgId != null && currentChatMessageId != null && msgId == currentChatMessageId
        }
        if (foundMsg == null) {
            queue.add(event)
        }
        queueMap[event.channel] = queue
    }


    fun processQueueForScheduledEvent() {
        if (isQueueProcess.not() && queueMap.isNotEmpty()) {
            isQueueProcess = true
            val publishedEvents = arrayListOf<ClientMessage>()
            queueMap.keys.forEach {
                val queue = queueMap[it]
                queue?.let {
                    val count = queue.count()
                    var check = 0
                    while (check < count) {
                        val event = queue.peek()
                        event?.let {
                            when {
                                shouldPublishEvent(event) -> publishedEvents.add(queue.remove()!!)
                                shouldDismissEvent(event) -> {
                                    logDismissedEvent(event)
                                    queue.remove()
                                }
                                else -> {
                                }
                            }
                        }
                        check++
                    }
                }
            }
            listener?.onClientMessageEvents(this, publishedEvents)
            isQueueProcess = false
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

    override fun destroy() {
        super.destroy()
        publishSyncMessagesJob.cancel()

    }
}

// Extension for MessagingClient to be synced
internal fun MessagingClient.syncTo(
    timeSource: () -> EpochTime,
    validEventBufferMs: Long = Long.MAX_VALUE
): SynchronizedMessagingClient {
    return SynchronizedMessagingClient(this, timeSource, validEventBufferMs)
}
