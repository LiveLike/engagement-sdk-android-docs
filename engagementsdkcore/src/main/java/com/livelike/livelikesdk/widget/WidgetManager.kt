package com.livelike.livelikesdk.widget

import com.livelike.engagementsdkapi.Stream
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.services.messaging.proxies.ExternalMessageTrigger
import com.livelike.livelikesdk.services.messaging.proxies.ExternalTriggerListener
import com.livelike.livelikesdk.services.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.services.messaging.proxies.TriggeredMessagingClient

// / Transforms ClientEvent into WidgetViews and sends to WidgetRenderer
internal class WidgetManager(
    upstream: MessagingClient,
    private val dataClient: WidgetDataClient,
    private val widgetInfosStream: Stream<WidgetInfos?>
) :
    MessagingClientProxy(upstream),
    ExternalMessageTrigger {

    init {
        widgetInfosStream.subscribe(this::class.java) { widgetInfos: WidgetInfos? ->
            isProcessing = (widgetInfos != null)
            if (!isProcessing) {
                triggerListener?.onTrigger("done")
            }
        }
    }

    override var isProcessing: Boolean = false
    override var triggerListener: ExternalTriggerListener? = null

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        val widgetType = event.message.get("event").asString ?: ""
        val payload = event.message["payload"].asJsonObject

        // Filter only valid widget types here
        if (WidgetType.fromString(widgetType) != WidgetType.NONE) {
            widgetInfosStream.onNext(WidgetInfos(widgetType, payload))

            // Register the impression on the backend
            payload.get("impression_url")?.asString?.let {
                dataClient.registerImpression(it)
            }

            super.onClientMessageEvent(client, event)
        }
    }
}

enum class WidgetType(val value: String) {
    TEXT_PREDICTION("text-prediction-created"),
    TEXT_PREDICTION_FOLLOW_UP("text-prediction-follow-up-created"),
    IMAGE_PREDICTION("image-prediction-created"),
    IMAGE_PREDICTION_FOLLOW_UP("image-prediction-follow-up-created"),
    HTML5("html-widget"),
    TEXT_QUIZ("text-quiz-created"),
    IMAGE_QUIZ("image-quiz-created"),
    TEXT_POLL("text-poll-created"),
    IMAGE_POLL("image-poll-created"),
    ALERT("alert-created"),
    NONE("none");

    companion object {
        private val map = values().associateBy(WidgetType::value)
        fun fromString(type: String) = map[type] ?: NONE
    }
}

internal interface WidgetDataClient {
    fun vote(voteUrl: String)
    fun vote(voteUrl: String, voteUpdateCallback: ((String) -> Unit)?)
    fun changeVote(voteUrl: String, newVoteId: String, voteUpdateCallback: ((String) -> Unit)?)
    fun registerImpression(impressionUrl: String)
    fun fetchQuizResult(answerUrl: String)
}

internal fun MessagingClient.asWidgetManager(
    dataClient: WidgetDataClient,
    widgetInfosStream: Stream<WidgetInfos?>
): WidgetManager {
    val triggeredMessagingClient = TriggeredMessagingClient(this)
    val widgetQueue =
        WidgetManager(triggeredMessagingClient, dataClient, widgetInfosStream)
    triggeredMessagingClient.externalTrigger = widgetQueue
    return widgetQueue
}
