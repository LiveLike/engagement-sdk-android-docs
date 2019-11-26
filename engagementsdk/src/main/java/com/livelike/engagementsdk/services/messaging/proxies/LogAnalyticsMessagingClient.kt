package com.livelike.engagementsdk.services.messaging.proxies

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.MessagingClient

/**
 * Meessaging Proxy/Pipe for adding analytics for our widgets received.
*/
internal class LogAnalyticsMessagingClient(
    upstream: MessagingClient,
    val analyticsService: AnalyticsService
) :
    MessagingClientProxy(upstream) {
    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        upstream.stop()
    }

    override fun start() {
        upstream.start()
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        listener?.onClientMessageEvent(client, event)
        val widgetType = event.message.get("event").asString ?: ""
        val payload = event.message["payload"].asJsonObject
        val widgetId = payload["id"].asString
        analyticsService.trackWidgetReceived(widgetType, widgetId)
    }
}

internal fun MessagingClient.logAnalytics(
    analyticsService: AnalyticsService
): LogAnalyticsMessagingClient {
    return LogAnalyticsMessagingClient(this, analyticsService)
}
