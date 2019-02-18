package com.livelike.livelikesdk.messaging.proxies

import com.livelike.livelikesdk.messaging.*

//TODO Look into removing MessageClientProxy and replacing with Kotlin MessageClient by upstream
abstract class MessagingClientProxy (val upstream: MessagingClient) : MessagingClient, MessagingEventListener {

    var listener : MessagingEventListener? = null

    init {
        upstream.addMessagingEventListener(this)
    }

    override fun subscribe(channels: List<String>) {
        upstream.subscribe(channels)
    }

    override fun unsubscribe(channels: List<String>) {
        upstream.unsubscribe(channels)
    }

    override fun sendMessage(message: ClientMessage) {
        upstream.sendMessage(message)
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