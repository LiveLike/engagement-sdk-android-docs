package com.livelike.livelikesdk.widget

import android.util.Log
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.proxies.ExternalMessageTrigger
import com.livelike.livelikesdk.messaging.proxies.ExternalTriggerListener
import com.livelike.livelikesdk.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.messaging.proxies.TriggeredMessagingClient

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
        //Process payload for widgets and send to Renderer (WidgetView)
        val widgetType = WidgetType.fromString(event.message.get("event").asString ?: "")
        when (widgetType) {
            WidgetType.HTML5 -> renderer?.displayWidget(event.message["payload"].asJsonObject["url"])
            WidgetType.IMAGE_PREDICTION -> TODO()
            WidgetType.TEXT_PREDICTION_RESULTS -> TODO()
            WidgetType.TEXT_PREDICTION -> TODO()
            else -> {
            }
        }

        super.onClientMessageEvent(client, event)
    }

    fun toggleEmission(pause: Boolean) {
        Log.i(javaClass::getSimpleName.name, "The session has been " + if (pause) "paused" else "resumed" )
        triggerListener?.toggleEmission(pause)
        // When the session is paused, if a widget was on screen, he is dismissed
        if (pause){
            renderer?.dismissCurrentWidget()
        }
    }
}

enum class WidgetType (val value: String) {
    TEXT_PREDICTION("textPrediction"), //Examples we need real names here
    TEXT_PREDICTION_RESULTS("textPredictionResults"),
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
    fun displayWidget(widgetData:Any)
    fun dismissCurrentWidget()
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

