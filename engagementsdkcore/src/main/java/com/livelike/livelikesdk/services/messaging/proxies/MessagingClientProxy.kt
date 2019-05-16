package com.livelike.livelikesdk.services.messaging.proxies

import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.ConnectionStatus
import com.livelike.livelikesdk.services.messaging.Error
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.services.messaging.MessagingEventListener

// TODO Look into removing MessageClientProxy and replacing with Kotlin MessageClient by upstream
internal abstract class MessagingClientProxy(val upstream: MessagingClient) : MessagingClient, MessagingEventListener {

    var listener: MessagingEventListener? = null

    init {
        upstream.addMessagingEventListener(this)
    }

    override fun subscribe(channels: List<String>) {
        upstream.subscribe(channels)
    }

    override fun unsubscribe(channels: List<String>) {
        upstream.unsubscribe(channels)
    }

    override fun unsubscribeAll() {
        upstream.unsubscribeAll()
    }

    override fun addMessagingEventListener(listener: MessagingEventListener) {
        this.listener = listener
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        listener?.onClientMessageEvent(client, event)
    }

    override fun onClientMessageError(client: MessagingClient, error: Error) {
        listener?.onClientMessageError(client, error)
    }

    override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {
        listener?.onClientMessageStatus(client, status)
    }
}