package com.livelike.engagementsdk.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.gson.JsonObject
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.data.repository.UserRepository
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.proxies.MessagingClientProxy
import com.livelike.engagementsdk.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.services.network.WidgetDataClient
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.getTotalPoints
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.utils.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.PriorityQueue
import java.util.Queue

internal class WidgetManager(
    upstream: MessagingClient,
    private val dataClient: WidgetDataClient,
    private val currentWidgetViewStream: Stream<SpecifiedWidgetView?>,
    private val context: Context,
    private val widgetInterceptorStream: Stream<WidgetInterceptor>,
    private val analyticsService: AnalyticsService,
    private val sdkConfiguration: EngagementSDK.SdkConfiguration,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository,
    val animationEventsStream: SubscriptionManager<ViewAnimationEvents>
) :
    MessagingClientProxy(upstream) {

    data class MessageHolder(
        val messagingClient: MessagingClient,
        val clientMessage: ClientMessage
    ) : Comparable<MessageHolder> {
        override fun compareTo(other: MessageHolder): Int {
            val thisRank = this.clientMessage.message.get("priority")?.asInt ?: 0
            val otherRank = other.clientMessage.message.get("priority")?.asInt ?: 0
            return otherRank.compareTo(thisRank)
        }
    }

    private val messageQueue: Queue<MessageHolder> = PriorityQueue()
    private var widgetOnScreen = false
    private var pendingMessage: MessageHolder? = null

    init {
        // TODO BUG : unsubscribe old widget interceptor events. ANDSDK-468
        widgetInterceptorStream.subscribe(javaClass) { wi ->
            wi?.events?.subscribe(javaClass.simpleName) {
                when (it) {
                    WidgetInterceptor.Decision.Show -> showPendingMessage()
                    WidgetInterceptor.Decision.Dismiss -> dismissPendingMessage()
                }
            }
        }
    }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        currentWidgetViewStream.onNext(null)
        upstream.stop()
    }

    override fun resume() {
        upstream.resume()
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        messageQueue.add(MessageHolder(client, event))
        if (!widgetOnScreen) {
            publishNextInQueue()
        }
    }

    private fun publishNextInQueue() {
        if (messageQueue.isNotEmpty()) {
            widgetOnScreen = true
            notifyIntegrator(messageQueue.remove())
        } else {
            widgetOnScreen = false
            currentWidgetViewStream.onNext(null) // sometimes widget view obscure the whole screen chat due t which app seems unresponsive
        }
    }

    private fun showPendingMessage() {
        pendingMessage?.let {
            showWidgetOnScreen(it)
        }
    }

    private fun dismissPendingMessage() {
        publishNextInQueue()
    }

    private fun notifyIntegrator(message: MessageHolder) {
        val widgetType =
            WidgetType.fromString(message.clientMessage.message.get("event").asString ?: "")
        if (widgetInterceptorStream.latest() == null || widgetType == WidgetType.POINTS_TUTORIAL || widgetType == WidgetType.COLLECT_BADGE) {
            showWidgetOnScreen(message)
        } else {
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    // Need to assure we are on the main thread to communicated with the external activity
                    try {
                        widgetInterceptorStream.latest()?.widgetWantsToShow()
                    } catch (e: Exception) {
                        logError { "Widget interceptor encountered a problem: $e \n Dismissing the widget" }
                        dismissPendingMessage()
                    }
                }
            }
            pendingMessage = message
        }
    }

    private fun showWidgetOnScreen(msgHolder: MessageHolder) {
        val widgetType = msgHolder.clientMessage.message.get("event").asString ?: ""
        val payload = msgHolder.clientMessage.message["payload"].asJsonObject
        val widgetId = payload["id"].asString

        analyticsService.trackWidgetDisplayed(widgetType, widgetId)

        handler.post {
            currentWidgetViewStream.onNext(
                WidgetProvider().get(
                    this,
                    WidgetInfos(widgetType, payload, widgetId),
                    context,
                    analyticsService,
                    sdkConfiguration,
                    {
                        checkForPointTutorial()
                        publishNextInQueue()
                    },
                    userRepository,
                    programRepository,
                    animationEventsStream
                )
            )
        }

        // Register the impression on the backend
        payload.get("impression_url")?.asString?.let {
            dataClient.registerImpression(it)
        }

        super.onClientMessageEvent(msgHolder.messagingClient, msgHolder.clientMessage)
    }

    private fun checkForPointTutorial() {
        if (shouldShowPointTutorial()) {
            // Check if user scored points
            if (getTotalPoints() != 0) {
                val message = ClientMessage(
                    JsonObject().apply {
                        addProperty("event", "points-tutorial")
                        add("payload", JsonObject().apply {
                            addProperty("id", "gameification")
                        })
                        addProperty("priority", 3)
                    }
                )
                onClientMessageEvent(this, message)
            }
        }
    }
}

enum class WidgetType(val event: String) {
    CHEER_METER("cheer-meter-created"),
    TEXT_PREDICTION("text-prediction-created"),
    TEXT_PREDICTION_FOLLOW_UP("text-prediction-follow-up-updated"),
    IMAGE_PREDICTION("image-prediction-created"),
    IMAGE_PREDICTION_FOLLOW_UP("image-prediction-follow-up-updated"),
    TEXT_QUIZ("text-quiz-created"),
    IMAGE_QUIZ("image-quiz-created"),
    TEXT_POLL("text-poll-created"),
    IMAGE_POLL("image-poll-created"),
    POINTS_TUTORIAL("points-tutorial"),
    COLLECT_BADGE("collect-badge"),
    ALERT("alert-created");

    companion object {
        private val map = values().associateBy(WidgetType::event)
        fun fromString(type: String) = map[type]
    }
}

internal fun MessagingClient.asWidgetManager(
    dataClient: WidgetDataClient,
    widgetInfosStream: SubscriptionManager<SpecifiedWidgetView?>,
    context: Context,
    widgetInterceptorStream: Stream<WidgetInterceptor>,
    analyticsService: AnalyticsService,
    sdkConfiguration: EngagementSDK.SdkConfiguration,
    userRepository: UserRepository,
    programRepository: ProgramRepository,
    animationEventsStream: SubscriptionManager<ViewAnimationEvents>
): WidgetManager {
    return WidgetManager(
        this,
        dataClient,
        widgetInfosStream,
        context,
        widgetInterceptorStream,
        analyticsService,
        sdkConfiguration,
        userRepository,
        programRepository,
        animationEventsStream
    )
}
