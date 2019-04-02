package com.livelike.livelikesdk.messaging.proxies

import android.content.Context
import android.os.Handler
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.ConnectionStatus
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.util.Queue
import com.livelike.livelikesdk.util.logVerbose

internal class SynchronizedMessagingClient(
    upstream: MessagingClient,
    var timeSource: () -> EpochTime,
    val validEventBufferMs: Long
) :
    MessagingClientProxy(upstream) {

    companion object {
        const val SYNC_TIME_FIDELITY = 100L
    }

    var activeSub = false
    private val queue = Queue<ClientMessage>()
    private val timerTask = Runnable { processQueueForScheduledEvent() }
    private val timer = SyncTimer(timerTask, SYNC_TIME_FIDELITY)

    override fun subscribe(channels: List<String>) {
        activeSub = true
        if(!timer.running)
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
            shouldDismissEvent(event) -> return
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
                logVerbose { "Dismissed Client Message -- the message was too old!" }
                queue.dequeue()
            }
        }
    }

    fun publishEvent(event: ClientMessage) {
        logVerbose { "Publish ClientMessage" }
        listener?.onClientMessageEvent(this, event)
    }

    fun shouldPublishEvent(event: ClientMessage) : Boolean =
         event.timeStamp <= EpochTime(0) ||
                (event.timeStamp <= timeSource() && event.timeStamp >= timeSource() - validEventBufferMs)


    fun shouldDismissEvent(event: ClientMessage) : Boolean =
         event.timeStamp > EpochTime(0) &&
                (event.timeStamp < timeSource() - validEventBufferMs)

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

//Extension for MessagingClient to be synced
internal fun MessagingClient.syncTo(
    timeSource: () -> EpochTime,
    validEventBufferMs: Long = 10000L
): SynchronizedMessagingClient {
    return SynchronizedMessagingClient(this, timeSource, validEventBufferMs)
}

//Extension for MessagingClient to be synced
internal fun MessagingClient.withPreloader(
    context: Context
): ImagePreloaderMessagingClient {
    return ImagePreloaderMessagingClient(this, context)
}