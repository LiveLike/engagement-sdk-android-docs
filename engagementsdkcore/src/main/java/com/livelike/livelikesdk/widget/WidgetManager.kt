package com.livelike.livelikesdk.widget

import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdkapi.WidgetEvent
import com.livelike.engagementsdkapi.WidgetEventListener
import com.livelike.engagementsdkapi.WidgetRenderer
import com.livelike.engagementsdkapi.WidgetStateProcessor
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.proxies.ExternalMessageTrigger
import com.livelike.livelikesdk.messaging.proxies.ExternalTriggerListener
import com.livelike.livelikesdk.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.messaging.proxies.TriggeredMessagingClient

/// Transforms ClientEvent into WidgetViews and sends to WidgetRenderer
internal class WidgetManager(upstream: MessagingClient, private val dataClient: WidgetDataClient, private val stateProcessor: WidgetStateProcessor) :
        MessagingClientProxy(upstream),
        ExternalMessageTrigger,
    WidgetEventListener {
    private val exemptionList = listOf(
        Pair("event", WidgetType.TEXT_QUIZ_RESULT.value),
        Pair("event", WidgetType.IMAGE_QUIZ_RESULT.value),
        Pair("event", WidgetType.TEXT_POLL_RESULT.value),
        Pair("event", WidgetType.IMAGE_POLL_RESULT.value))

    private val widgetSubscribedChannels = mutableListOf<String>()
    private var processingVoteUpdate = false
    private var voteUpdateHandler: Handler = Handler()

    override fun onWidgetDisplayed(impressionUrl: String) {
        dataClient.registerImpression(impressionUrl)
    }

    var renderer: WidgetRenderer? = null
    set(value) {
        field = value
        value?.widgetListener = this
        value?.widgetStateProcessor = stateProcessor
    }

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
            else -> {}
        }
    }

    override fun subscribeForResults(channel: String) {
        if(channel.isNotEmpty() && !widgetSubscribedChannels.contains(channel)) {
            widgetSubscribedChannels.add(channel)
            upstream.subscribe(listOf(channel))
        }
    }

    override fun onOptionVote(voteUrl: String, channel: String, voteChangeCallback: ((String) -> Unit)?) {
        dataClient.vote(voteUrl, voteChangeCallback)
        subscribeForResults(channel)
    }

    override fun onOptionVoteUpdate(oldVoteUrl:String, newVoteId:String, channel: String, voteUpdateCallback: ((String)-> Unit)?) {
        if(processingVoteUpdate) {
            voteUpdateHandler.removeCallbacksAndMessages(null)
            voteUpdateHandler.postDelayed({ onOptionVoteUpdate(oldVoteUrl, newVoteId, channel, voteUpdateCallback) }, 200)
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
        val exemption = exemptionList.any { event.message[it.first].asString == it.second }
        //If this message type is in the exemption list it should never flip processing boolean
        isProcessing = !exemption || isProcessing
        val widgetType = event.message.get("event").asString ?: ""
        val payload = event.message["payload"].asJsonObject
        Handler(Looper.getMainLooper()).post {
            renderer?.displayWidget(widgetType, payload, WidgetTransientState())
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
        private val map = WidgetType.values().associateBy(WidgetType::value)
        fun fromString(type: String) = map[type] ?: NONE
    }
}


internal interface WidgetDataClient {
    fun vote(voteUrl:String, voteUpdateCallback: ((String) -> Unit)?)
    fun changeVote(voteUrl:String, newVoteId: String, voteUpdateCallback: ((String) -> Unit)?)
    fun registerImpression(impressionUrl: String)
    fun fetchQuizResult(answerUrl: String)
}

internal fun MessagingClient.asWidgetManager(
    dataClient: WidgetDataClient,
    stateProcessor: WidgetStateProcessor
): WidgetManager {
    val triggeredMessagingClient = TriggeredMessagingClient(this)
    val widgetQueue = WidgetManager(triggeredMessagingClient, dataClient, stateProcessor)
    triggeredMessagingClient.externalTrigger = widgetQueue
    return widgetQueue
}