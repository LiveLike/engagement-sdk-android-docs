package com.livelike.livelikesdk.messaging.proxies

import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.ConnectionStatus
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.util.Queue
import java.util.*


class SynchronizedMessagingClient(upstream: MessagingClient, var timeSource: () -> EpochTime) :
    MessagingClientProxy(upstream) {
    companion object {
        const val SYNC_TIME_FIDELITY = 100L
    }

    var activeSub = false
    private val queue = Queue<ClientMessage>()
    private val timer = SyncTimer()
    private val timerTask = object : TimerTask() {
        override fun run() {
            processQueueForScheduledEvent()
        }
    }

    override fun subscribe(channels: List<String>) {
        activeSub = true
        if(!timer.running)
            timer.scheduleAtFixedRate(timerTask, SYNC_TIME_FIDELITY, SYNC_TIME_FIDELITY)
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
            ConnectionStatus.CONNECTED -> if(activeSub) timer.scheduleAtFixedRate(timerTask, SYNC_TIME_FIDELITY, SYNC_TIME_FIDELITY)
            ConnectionStatus.DISCONNECTED -> timer.cancel()
        }
    }

    fun processQueueForScheduledEvent() {
        val event = queue.peek()?:return
        //For now lets use the timestamp, we can implement minimumTime when sync timing comes in, timestamp of <= 0 is passthrough
        if(event.timeStamp > EpochTime(0)) {
            if (event.timeStamp <= timeSource()) {
                publishEvent(queue.dequeue()!!)
            }
        } else {
            publishEvent(queue.dequeue()!!)
        }
    }

    fun publishEvent(event: ClientMessage) {
        listener?.onClientMessageEvent(this, event)
    }
}

class SyncTimer : Timer() {
    var running = false
    override fun scheduleAtFixedRate(task: TimerTask?, delay: Long, period: Long) {
        if (running) return
        super.scheduleAtFixedRate(task, delay, period)
        running = true
    }

    override fun cancel() {
        running = false
        super.cancel()
    }
}

//Extension for MessagingClient to be synced
fun MessagingClient.syncTo(timeSource: () -> EpochTime): SynchronizedMessagingClient {
    return SynchronizedMessagingClient(this, timeSource)
}