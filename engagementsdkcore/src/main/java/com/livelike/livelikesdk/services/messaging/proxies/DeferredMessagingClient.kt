package com.livelike.livelikesdk.services.messaging.proxies

import com.livelike.engagementsdkapi.EpochTime
import com.livelike.livelikesdk.Stream
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.utils.SubscriptionManager
import java.util.LinkedList
import java.util.Queue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DeferredMessagingClient(
    upstream: MessagingClient,
    private val widgetInterceptor: WidgetInterceptor?
) :
    MessagingClientProxy(upstream) {

    private val pendingMessageQueue: Queue<ClientMessage> = LinkedList()

    init {
        widgetInterceptor?.events?.subscribe(javaClass.simpleName) {
            when (it) {
                WidgetInterceptor.Decision.Show -> showPendingMessages()
                WidgetInterceptor.Decision.Dismiss -> dismissPendingMessages()
            }
        }
    }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        upstream.stop()
    }

    override fun resume() {
        upstream.resume()
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        if (widgetInterceptor != null) {
            pendingMessageQueue.add(event)
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    // Need to assure we are on the main thread to communicated with the external activity
                    widgetInterceptor.widgetWantsToShow()
                }
            }
        } else {
            listener?.onClientMessageEvent(client, event)
        }
    }

    private fun showPendingMessages() {
        pendingMessageQueue.forEach {
            listener?.onClientMessageEvent(this, it)
        }
        pendingMessageQueue.clear()
    }

    private fun dismissPendingMessages() {
        pendingMessageQueue.clear()
    }
}

abstract class WidgetInterceptor {
    abstract fun widgetWantsToShow()
    val events: Stream<Decision> = SubscriptionManager()
    fun showWidget() {
        events.onNext(Decision.Show)
    }
    fun dismissWidget() {
        events.onNext(Decision.Dismiss)
    }
    enum class Decision {
        Show,
        Dismiss
    }
}

internal fun MessagingClient.integratorDeferredClient(
    widgetInterceptor: WidgetInterceptor?
): DeferredMessagingClient {
    return DeferredMessagingClient(this, widgetInterceptor)
}
