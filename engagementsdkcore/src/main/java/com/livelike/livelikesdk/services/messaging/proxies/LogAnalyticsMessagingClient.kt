package com.livelike.livelikesdk.services.messaging.proxies

import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.MessagingClient

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

    override fun resume() {
        upstream.resume()
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
