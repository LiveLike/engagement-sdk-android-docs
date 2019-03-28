package com.livelike.livelikesdk.widget

import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdkapi.WidgetEvent
import com.livelike.engagementsdkapi.WidgetEventListener
import com.livelike.engagementsdkapi.WidgetRenderer
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.proxies.ExternalMessageTrigger
import com.livelike.livelikesdk.messaging.proxies.ExternalTriggerListener
import com.livelike.livelikesdk.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.messaging.proxies.TriggeredMessagingClient

/// Transforms ClientEvent into WidgetViews and sends to WidgetRenderer
internal class WidgetManager(upstream: MessagingClient, private val dataClient: WidgetDataClient) :
        MessagingClientProxy(upstream),
        ExternalMessageTrigger,
    WidgetEventListener {
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
        val widgetType = event.message.get("event").asString ?: ""
        val payload = event.message["payload"].asJsonObject
        Handler(Looper.getMainLooper()).post {
            renderer?.displayWidget(widgetType, payload)
        }
        super.onClientMessageEvent(client, event)
    }

    fun toggleEmission(pause: Boolean) {
        triggerListener?.toggleEmission(pause)
        if (pause){
            renderer?.dismissCurrentWidget()
        }
    }

    // TODO: Name should be changed to more generic and avoid terms like analytics.
    interface WidgetAnalyticsObserver {
        fun widgetDismissed(widgetId: String, kind: String)
        fun widgetShown(widgetId: String, kind: String)
        fun widgetOptionSelected(widgetId: String, kind: String)
    }
}

enum class WidgetType (val value: String) {
    TEXT_PREDICTION("text-prediction-created"),
    TEXT_PREDICTION_RESULTS("text-prediction-follow-up-created"),
    IMAGE_PREDICTION("image-prediction-created"),
    IMAGE_PREDICTION_RESULTS("image-prediction-follow-up-created"),
    HTML5("html-widget"),
    ALERT("alert-created"),
    NONE("none");

    companion object {
        private val map = WidgetType.values().associateBy(WidgetType::value)
        fun fromString(type: String) = map[type] ?: NONE
    }
}


internal interface WidgetDataClient {
    fun vote(voteUrl:String)
}

internal fun MessagingClient.asWidgetManager(dataClient: WidgetDataClient): WidgetManager {
    val triggeredMessagingClient = TriggeredMessagingClient(this)
    val widgetQueue = WidgetManager(triggeredMessagingClient, dataClient)
    triggeredMessagingClient.externalTrigger = widgetQueue
    return widgetQueue
}