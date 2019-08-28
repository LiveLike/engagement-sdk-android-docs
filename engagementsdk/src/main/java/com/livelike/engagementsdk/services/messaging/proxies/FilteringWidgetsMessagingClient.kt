package com.livelike.engagementsdk.services.messaging.proxies

import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Resource

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
            val widgetType = WidgetType.fromString(event.message.get("event").asString ?: "")
            val payload = event.message.get("payload").asJsonObject
            val resource = gson.fromJson(payload, Resource::class.java) ?: null

            resource?.let {
                when (widgetType) {
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
                        if (widgetType != null) {
                            listener?.onClientMessageEvent(client, event)
                        } else {
                            // Do nothing, filter this event
                        }
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
