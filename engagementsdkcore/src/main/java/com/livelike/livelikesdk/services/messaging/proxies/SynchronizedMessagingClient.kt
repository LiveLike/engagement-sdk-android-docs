package com.livelike.livelikesdk.services.messaging.proxies

import android.os.Handler
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.ConnectionStatus
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.utils.Queue
import com.livelike.livelikesdk.utils.logVerbose

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

    var activeSub = false
    private val queue = Queue<ClientMessage>()
    private val timerTask = Runnable { processQueueForScheduledEvent() }
    private val timer = SyncTimer(timerTask, SYNC_TIME_FIDELITY)

    override fun subscribe(channels: List<String>) {
        activeSub = true
        if (!timer.running)
            timer.start()
        super.subscribe(channels)
    }

    override fun unsubscribeAll() {
        activeSub = false
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

    override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {
        super.onClientMessageStatus(client, status)
        when (status) {
            ConnectionStatus.CONNECTED -> if (activeSub) timer.start()
            ConnectionStatus.DISCONNECTED -> timer.cancel()
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

internal class SyncTimer(val task: Runnable, val period: Long) {
    var running = false
    val handler = Handler()
    private var innerRunnable = Runnable {
        doTick()
    }

    fun start() {
        if (running) return
        handler.postDelayed(innerRunnable, period)
        running = true
    }

    fun cancel() {
        running = false
        handler.removeCallbacks(innerRunnable)
    }

    private fun doTick() {
        if (running) {
            handler.post(task)
            handler.postDelayed(innerRunnable, period)
        }
    }
}

// Extension for MessagingClient to be synced
internal fun MessagingClient.syncTo(
    timeSource: () -> EpochTime,
    validEventBufferMs: Long = Long.MAX_VALUE
): SynchronizedMessagingClient {
    return SynchronizedMessagingClient(this, timeSource, validEventBufferMs)
}
