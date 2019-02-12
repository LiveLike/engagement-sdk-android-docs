package com.livelike.livelikesdk.messaging.proxies

import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.util.Queue


class TriggeredMessagingClient(upstream: MessagingClient) :
        MessagingClientProxy(upstream), ExternalTriggerListener {

    val queue = Queue<ClientMessage>()
    var externalTrigger: ExternalMessageTrigger = EmptyTrigger()
    set(value) {
        field = value
        value.triggerListener = this
    }

    override fun onTrigger(data: Any) {
        val event = queue.dequeue()?:return
        listener?.onClientMessageEvent(this, event)
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        if(externalTrigger.isProcessing)
            queue.enqueue(event)
        else
            super.onClientMessageEvent(client, event)
    }
}

class EmptyTrigger : ExternalMessageTrigger {
    override var triggerListener: ExternalTriggerListener? = null
    override var isProcessing = false
}

interface ExternalTriggerListener {
    fun onTrigger(data: Any)
}

interface ExternalMessageTrigger {
    var isProcessing: Boolean
    var triggerListener: ExternalTriggerListener?
}