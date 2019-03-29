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
import com.livelike.livelikesdk.util.logVerbose

/// Transforms ClientEvent into WidgetViews and sends to WidgetRenderer
internal class WidgetManager(upstream: MessagingClient, private val dataClient: WidgetDataClient) :
        MessagingClientProxy(upstream),
        ExternalMessageTrigger,
    WidgetEventListener {
    override fun onWidgetDisplayed(impressionUrl: String) {
        dataClient.registerImpression(impressionUrl)
    }

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

    override fun onOptionVote(voteUrl: String, channel: String) {
        if (channel.isNotEmpty()) upstream.subscribe(listOf(channel))
        if (voteUrl == "null" || voteUrl.isEmpty()) {
            return
        }
        logVerbose { "Voting for $voteUrl" }
        dataClient.vote(voteUrl)

        dataClient.vote(voteUrl)
        if (channel.isNotEmpty()) upstream.subscribe(listOf(channel))
    }

    override fun onFetchingQuizResult(answerUrl: String) {
        isProcessing = false
        dataClient.fetchQuizResult(answerUrl)
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
}

enum class WidgetType (val value: String) {
    TEXT_PREDICTION("text-prediction-created"),
    TEXT_PREDICTION_RESULTS("text-prediction-follow-up-created"),
    IMAGE_PREDICTION("image-prediction-created"),
    IMAGE_PREDICTION_RESULTS("image-prediction-follow-up-created"),
    HTML5("html-widget"),
    IMAGE_QUIZ("image-quiz-created"),
    IMAGE_QUIZ_RESULT("image-quiz-results"),
    ALERT("alert-created"),
    NONE("none");

    companion object {
        private val map = WidgetType.values().associateBy(WidgetType::value)
        fun fromString(type: String) = map[type] ?: NONE
    }
}


internal interface WidgetDataClient {
    fun vote(voteUrl:String)
    fun registerImpression(impressionUrl: String)
    fun fetchQuizResult(answerUrl: String)
}

internal fun MessagingClient.asWidgetManager(dataClient: WidgetDataClient): WidgetManager {
    val triggeredMessagingClient = TriggeredMessagingClient(this)
    val widgetQueue = WidgetManager(triggeredMessagingClient, dataClient)
    triggeredMessagingClient.externalTrigger = widgetQueue
    return widgetQueue
}