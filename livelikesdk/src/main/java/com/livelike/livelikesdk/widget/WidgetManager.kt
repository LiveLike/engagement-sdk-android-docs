package com.livelike.livelikesdk.widget

import com.google.gson.JsonObject
import com.livelike.livelikesdk.analytics.InteractionLogger
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.proxies.ExternalMessageTrigger
import com.livelike.livelikesdk.messaging.proxies.ExternalTriggerListener
import com.livelike.livelikesdk.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.messaging.proxies.TriggeredMessagingClient
import com.livelike.livelikesdk.widget.view.WidgetEventListener

/// Transforms ClientEvent into WidgetViews and sends to WidgetRenderer
class WidgetManager(upstream: MessagingClient, val dataClient: WidgetDataClient) :
        MessagingClientProxy(upstream),
        ExternalMessageTrigger,
        WidgetEventListener{
    private val analyticsListeners = HashSet<WidgetAnalyticsObserver>()
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

    override fun onOptionVote(voteUrl: String) {
        dataClient.vote(voteUrl)
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        isProcessing = true
        val widgetType = WidgetType.fromString(event.message.get("event").asString ?: "")
        val payload = event.message["payload"].asJsonObject
        renderer?.displayWidget(widgetType, payload, analyticsListeners)
        super.onClientMessageEvent(client, event)
    }

    fun toggleEmission(pause: Boolean) {
        triggerListener?.toggleEmission(pause)
        if (pause){
            renderer?.dismissCurrentWidget()
        }
    }

    fun subscribeAnalytics(observer: WidgetAnalyticsObserver) {
        analyticsListeners.add(observer)
    }

    fun unsubscribeAnalytics(observer: WidgetAnalyticsObserver) {
        if (analyticsListeners.contains(observer))
            analyticsListeners.remove(observer)
    }

    interface WidgetAnalyticsObserver {
        fun widgetDismissed(widgetId: String)
        fun widgetShown(widgetId: String)
        fun widgetOptionSelected(widgetId: String)
    }
}

enum class WidgetType (val value: String) {
    TEXT_PREDICTION("text-prediction-created"),
    TEXT_PREDICTION_RESULTS("text-prediction-follow-up-created"),
    IMAGE_PREDICTION("image-prediction-created"),
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
    fun displayWidget(
        type: WidgetType,
        payload: JsonObject,
        observerListeners: Set<WidgetManager.WidgetAnalyticsObserver>
    )
}

enum class WidgetEvent{
    WIDGET_DISMISS,
    WIDGET_SHOWN
}

interface WidgetDataClient {
    fun vote(voteUrl:String)
}

fun MessagingClient.asWidgetManager(dataClient: WidgetDataClient) : WidgetManager {
    val triggeredMessagingClient = TriggeredMessagingClient(this)
    val widgetQueue = WidgetManager(triggeredMessagingClient, dataClient)
    triggeredMessagingClient.externalTrigger = widgetQueue
    return widgetQueue
}