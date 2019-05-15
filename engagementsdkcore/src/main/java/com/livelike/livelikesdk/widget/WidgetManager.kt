package com.livelike.livelikesdk.widget

import android.os.Handler
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.Stream
import com.livelike.engagementsdkapi.WidgetEvent
import com.livelike.engagementsdkapi.WidgetEventListener
import com.livelike.engagementsdkapi.WidgetRenderer
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.proxies.ExternalMessageTrigger
import com.livelike.livelikesdk.messaging.proxies.ExternalTriggerListener
import com.livelike.livelikesdk.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.messaging.proxies.TriggeredMessagingClient

// / Transforms ClientEvent into WidgetViews and sends to WidgetRenderer
internal class WidgetManager(
    upstream: MessagingClient,
    private val dataClient: WidgetDataClient,
    private val widgetStream: Stream<String?, JsonObject?>
) :
    MessagingClientProxy(upstream),
    ExternalMessageTrigger,
    WidgetEventListener {

    init {
        widgetStream.subscribe(this::class.java) { s: String?, j: JsonObject? ->
            isProcessing = (s != null)
            if (!isProcessing) {
                triggerListener?.onTrigger("done")
            }
        }
    }

    private val exemptionList = listOf(
        Pair("event", WidgetType.TEXT_QUIZ_RESULT.value),
        Pair("event", WidgetType.IMAGE_QUIZ_RESULT.value),
        Pair("event", WidgetType.TEXT_POLL_RESULT.value),
        Pair("event", WidgetType.IMAGE_POLL_RESULT.value)
    )

    private val widgetSubscribedChannels = mutableListOf<String>()
    private var processingVoteUpdate = false
    private var voteUpdateHandler: Handler = Handler()

    override fun onWidgetDisplayed(impressionUrl: String) {
        dataClient.registerImpression(impressionUrl)
    }

    var renderer: WidgetRenderer? = null

    override var isProcessing: Boolean = false
    override var triggerListener: ExternalTriggerListener? = null
        set(listener) {
            field = listener
            listener?.exemptionList = exemptionList
        }

    override fun onWidgetEvent(event: WidgetEvent) {
        when (event) {
            WidgetEvent.WIDGET_DISMISS -> {
                isProcessing = false
                upstream.unsubscribe(widgetSubscribedChannels)
                widgetSubscribedChannels.clear()
                triggerListener?.onTrigger("done")
            }
            else -> {
            }
        }
    }

    override fun subscribeForResults(channel: String) {
        if (channel.isNotEmpty() && !widgetSubscribedChannels.contains(channel)) {
            widgetSubscribedChannels.add(channel)
            upstream.subscribe(listOf(channel))
        }
    }

    override fun onOptionVote(voteUrl: String, channel: String, voteChangeCallback: ((String) -> Unit)?) {
        if (processingVoteUpdate)
            return
        processingVoteUpdate = true
        dataClient.vote(voteUrl) {
            voteChangeCallback?.invoke(it)
            processingVoteUpdate = false
        }
        subscribeForResults(channel)
    }

    override fun onOptionVoteUpdate(
        oldVoteUrl: String,
        newVoteId: String,
        channel: String,
        voteUpdateCallback: ((String) -> Unit)?
    ) {
        if (processingVoteUpdate) {
            voteUpdateHandler.removeCallbacksAndMessages(null)
            voteUpdateHandler.postDelayed(
                { onOptionVoteUpdate(oldVoteUrl, newVoteId, channel, voteUpdateCallback) },
                200
            )
            return
        }
        processingVoteUpdate = true
        dataClient.changeVote(oldVoteUrl, newVoteId) {
            voteUpdateCallback?.invoke(it)
            processingVoteUpdate = false
        }
    }

    override fun onFetchingQuizResult(answerUrl: String) {
        dataClient.fetchQuizResult(answerUrl)
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        val widgetType = event.message.get("event").asString ?: ""
        val payload = event.message["payload"].asJsonObject
        payload.get("subscribe_channel")?.asString?.let {
            subscribeForResults(it)
        }

        widgetStream.onNext(widgetType, payload)

        // Register the impression on the backend
        payload.get("impression_url")?.asString?.let {
            dataClient.registerImpression(it)
        }

        super.onClientMessageEvent(client, event)
    }
}

enum class WidgetType(val value: String) {
    TEXT_PREDICTION("text-prediction-created"),
    TEXT_PREDICTION_RESULTS("text-prediction-follow-up-created"),
    IMAGE_PREDICTION("image-prediction-created"),
    IMAGE_PREDICTION_RESULTS("image-prediction-follow-up-created"),
    HTML5("html-widget"),
    TEXT_QUIZ("text-quiz-created"),
    TEXT_QUIZ_RESULT("text-quiz-results"),
    IMAGE_QUIZ("image-quiz-created"),
    IMAGE_QUIZ_RESULT("image-quiz-results"),
    TEXT_POLL("text-poll-created"),
    TEXT_POLL_RESULT("text-poll-results"),
    IMAGE_POLL("image-poll-created"),
    IMAGE_POLL_RESULT("image-poll-results"),
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
    widgetStream: Stream<String?, JsonObject?>
): WidgetManager {
    val triggeredMessagingClient = TriggeredMessagingClient(this)
    val widgetQueue =
        WidgetManager(triggeredMessagingClient, dataClient, widgetStream)
    triggeredMessagingClient.externalTrigger = widgetQueue
    return widgetQueue
}
