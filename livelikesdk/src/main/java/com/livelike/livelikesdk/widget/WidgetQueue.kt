package com.livelike.livelikesdk.widget

import android.util.Log
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.proxies.ExternalMessageTrigger
import com.livelike.livelikesdk.messaging.proxies.ExternalTriggerListener
import com.livelike.livelikesdk.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.messaging.proxies.TriggeredMessagingClient
import com.livelike.livelikesdk.widget.model.PredictionWidgetQuestionData
import com.livelike.livelikesdk.widget.model.WidgetData
import com.livelike.livelikesdk.util.extractLong
import com.livelike.livelikesdk.util.extractStringOrEmpty
import com.livelike.livelikesdk.widget.model.WidgetOptionsData
import java.net.URI
import java.util.*

/// Transforms ClientEvent into WidgetViews and sends to WidgetRenderer
class WidgetQueue(upstream: MessagingClient) :
        MessagingClientProxy(upstream),
        ExternalMessageTrigger,
        WidgetEventListener{
    var renderer: WidgetRenderer? = null
    set(value) {
        field = value
        value?.widgetListener = this
    }

    override var isProcessing: Boolean = false
    override var triggerListener: ExternalTriggerListener? = null

    override fun onAnalyticsEvent(data: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onWidgetEvent(event: WidgetEvent) {
        when (event) {
            WidgetEvent.WIDGET_DISMISS -> {
                isProcessing = false
                triggerListener?.onTrigger("done")
            }
            else -> {}
        }
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        isProcessing = true
        val widgetType = WidgetType.fromString(event.message.get("event").asString ?: "")
        when (widgetType) {

            WidgetType.HTML5 -> {

            }
            WidgetType.IMAGE_PREDICTION -> TODO()
            WidgetType.TEXT_PREDICTION_RESULTS -> {
                // Register view to get the updated widget data.
                // TODO: Parse json and fill questionData. Now dummy object created.
                //renderer?.bindViewWith(questionData, WidgetType.TEXT_PREDICTION_RESULTS)
                //renderer?.displayWidget()
                //renderer?.displayWidget(event.message["payload"].asJsonObject["url"])
                renderer?.displayWidget(widgetType, PredictionWidgetQuestionData())
            }
            WidgetType.TEXT_PREDICTION -> {
                //TODO: Lets move this parsing down to the renderer since that is what cares about this data
                // pass to it the type and raw data displayWidget(WidgetType, data: JsonObject)
                var data = PredictionWidgetQuestionData()
                val payload = event.message["payload"].asJsonObject
                data.question = payload.extractStringOrEmpty("question")

                val options = mutableListOf<WidgetOptionsData>()
                for(option in payload["options"].asJsonArray) {
                    val optionJson = option.asJsonObject;
                    options.add(WidgetOptionsData(
                        UUID.fromString(optionJson.extractStringOrEmpty("id")),
                        URI.create(optionJson.extractStringOrEmpty("vote_url")),
                        optionJson.extractStringOrEmpty("description"),
                        optionJson.extractLong("vote_count")))
                }
                data.optionList = options.toList()
                renderer?.displayWidget(widgetType, data)
            }
            else -> {
            }
        }

        super.onClientMessageEvent(client, event)
    }

    fun toggleEmission(pause: Boolean) {
        triggerListener?.toggleEmission(pause)
        if (pause){
            renderer?.dismissCurrentWidget()
        }
    }
}

enum class WidgetType (val value: String) {
    TEXT_PREDICTION("text-prediction-created"), //Examples we need real names here
    TEXT_PREDICTION_RESULTS("text-prediction-follow-up-created"),
    IMAGE_PREDICTION("imagePredictionResults"),
    HTML5("html-widget"),
    NONE("none");

    companion object {
        private val map = WidgetType.values().associateBy(WidgetType::value)
        fun fromString(type: String) = map[type] ?: NONE
    }
}

interface WidgetRenderer {
    var widgetListener: WidgetEventListener?
    fun dismissCurrentWidget()
    fun displayWidget(type: WidgetType, widgetData: WidgetData)
}

interface WidgetEventListener {
    fun onAnalyticsEvent(data: Any)
    fun onWidgetEvent(event: WidgetEvent)
}

enum class WidgetEvent{
    WIDGET_DISMISS,
    WIDGET_SHOWN
}


fun MessagingClient.toWidgetQueue() : WidgetQueue {
    val triggeredMessagingClient = TriggeredMessagingClient(this)
    val widgetQueue = WidgetQueue(triggeredMessagingClient)
    triggeredMessagingClient.externalTrigger = widgetQueue
    return widgetQueue
}

