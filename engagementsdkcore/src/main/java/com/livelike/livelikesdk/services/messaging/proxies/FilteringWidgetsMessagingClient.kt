package com.livelike.livelikesdk.services.messaging.proxies

import com.livelike.engagementsdkapi.EpochTime
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Resource

internal class FilteringWidgetsMessagingClient(
    upstream: MessagingClient
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
        try {
            val widgetType = event.message.get("event").asString ?: ""
            val payload = event.message.get("payload").asJsonObject
            val resource = gson.fromJson(payload, Resource::class.java) ?: null

            resource?.let {
                when (WidgetType.fromString(widgetType)) {
                    WidgetType.IMAGE_PREDICTION_FOLLOW_UP -> {
                        if (getWidgetPredictionVotedAnswerIdOrEmpty(resource.image_prediction_id).isNotEmpty()) {
                            listener?.onClientMessageEvent(client, event)
                        } else {
                            // Do nothing, filter this event
                        }
                    }
                    WidgetType.TEXT_PREDICTION_FOLLOW_UP -> {
                        if (getWidgetPredictionVotedAnswerIdOrEmpty(resource.text_prediction_id).isNotEmpty()) {
                            listener?.onClientMessageEvent(client, event)
                        } else {
                            // Do nothing, filter this event
                        }
                    }
                    else -> {
                        listener?.onClientMessageEvent(client, event)
                    }
                }
            }
        } catch (e: IllegalStateException) {
            listener?.onClientMessageEvent(client, event)
        }
    }
}

internal fun MessagingClient.filter(): FilteringWidgetsMessagingClient {
    return FilteringWidgetsMessagingClient(this)
}
