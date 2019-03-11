package com.livelike.livelikesdk.messaging.proxies

import android.os.Handler
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.ConnectionStatus
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.util.Queue
import com.livelike.livelikesdk.util.logVerbose

class SynchronizedMessagingClient(upstream: MessagingClient, var timeSource: () -> EpochTime) :
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
        queue.enqueue(event)
    }

    override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {
        super.onClientMessageStatus(client, status)
        when (status) {
            ConnectionStatus.CONNECTED -> if (activeSub) timer.start()
            ConnectionStatus.DISCONNECTED -> timer.cancel()
        }
    }

    private val validEventBufferMs = 10000L // 10sec

    fun processQueueForScheduledEvent() {
        val event = queue.peek()?:return
        //For now lets use the timestamp, we can implement minimumTime when sync timing comes in, timestamp of <= 0 is passthrough
//        logVerbose{"Event date  : ${Date(event.timeStamp.timeSinceEpochInMs)}"}
//        logVerbose{"Current date: ${Date(timeSource().timeSinceEpochInMs)}"}
        if(event.timeStamp > EpochTime(0)) {
            if (event.timeStamp <= timeSource()
                && event.timeStamp >= timeSource() - validEventBufferMs
            ) {
                logVerbose { "Publish Widget" }
                publishEvent(queue.dequeue()!!)
            } else if (event.timeStamp <= timeSource() - validEventBufferMs) {
                logVerbose { "Dismissed Widget -- the widget was too old!" }
                // Dequeue all events older than currentTime-validEventBuffer
                queue.dequeue()
            }
        } else {
            logVerbose { "Publish instant Widget" }
            publishEvent(queue.dequeue()!!)
        }
    }

    fun publishEvent(event: ClientMessage) {
        listener?.onClientMessageEvent(this, event)
    }
}

class SyncTimer(val task: Runnable, val period: Long) {
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
fun MessagingClient.syncTo(timeSource: () -> EpochTime): SynchronizedMessagingClient {
    return SynchronizedMessagingClient(this, timeSource)
}