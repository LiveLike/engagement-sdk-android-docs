package com.livelike.livelikesdk.services.messaging.proxies

import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.utils.Queue

internal class TriggeredMessagingClient(upstream: MessagingClient) :
    MessagingClientProxy(upstream), ExternalTriggerListener {
    private val queue = Queue<ClientMessage>()
    override var exemptionList: List<Pair<String, String>>? = null
    var externalTrigger: ExternalMessageTrigger = EmptyTrigger()
        set(value) {
            field = value
            value.triggerListener = this
        }
    private var isPaused = false

    override fun onTrigger(data: Any) {
        if (isPaused) return // Dismiss a direct widget received when the session is paused
        val event = queue.dequeue() ?: return
        listener?.onClientMessageEvent(this, event)
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        if (externalTrigger.isProcessing || isPaused)
            queue.enqueue(event)
        else
            super.onClientMessageEvent(client, event)
    }

    override fun toggleEmission(pause: Boolean) {
        isPaused = pause
    }
}

internal class EmptyTrigger : ExternalMessageTrigger {
    override var triggerListener: ExternalTriggerListener? = null
    override var isProcessing = false
}

interface ExternalTriggerListener {
    var exemptionList: List<Pair<String, String>>?
    fun onTrigger(data: Any)
    fun toggleEmission(pause: Boolean)
}

internal interface ExternalMessageTrigger {
    var isProcessing: Boolean
    var triggerListener: ExternalTriggerListener?
}