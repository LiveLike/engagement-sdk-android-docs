package com.livelike.livelikesdk.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.livelikesdk.EngagementSDK
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.Stream
import com.livelike.livelikesdk.WidgetInfos
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.services.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.services.messaging.proxies.WidgetInterceptor
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.utils.logError
import com.livelike.livelikesdk.widget.model.Reward
import java.util.PriorityQueue
import java.util.Queue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class WidgetManager(
    upstream: MessagingClient,
    private val dataClient: WidgetDataClient,
    private val currentWidgetViewStream: Stream<SpecifiedWidgetView?>,
    private val context: Context,
    private val session: LiveLikeContentSession,
    private val sdkConfiguration: EngagementSDK.SdkConfiguration
) :
    MessagingClientProxy(upstream) {

    data class MessageHolder(val messagingClient: MessagingClient, val clientMessage: ClientMessage) : Comparable<MessageHolder> {
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
        session.widgetInterceptor?.events?.subscribe(javaClass.simpleName) {
            when (it) {
                WidgetInterceptor.Decision.Show -> showPendingMessages()
                WidgetInterceptor.Decision.Dismiss -> dismissPendingMessages()
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
        }
    }

    private fun showPendingMessages() {
        pendingMessage?.let { showWidgetOnScreen(it) }
    }

    private fun dismissPendingMessages() {
        publishNextInQueue()
    }

    private fun notifyIntegrator(message: MessageHolder) {
        val widgetType = WidgetType.fromString(message.clientMessage.message.get("event").asString ?: "")
        if (session.widgetInterceptor == null || widgetType == WidgetType.POINTS_TUTORIAL) {
            showWidgetOnScreen(message)
        } else {
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    // Need to assure we are on the main thread to communicated with the external activity
                    try {
                        session.widgetInterceptor?.widgetWantsToShow()
                    } catch (e: Exception) {
                        logError { "The Widget interceptor encountered a problem: $e \n Releasing the widget." }
                        showWidgetOnScreen(message)
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

        session.analyticService.trackWidgetDisplayed(widgetType, widgetId)

        handler.post {
            currentWidgetViewStream.onNext(
                WidgetProvider().get(
                    WidgetInfos(widgetType, payload, widgetId),
                    context,
                    session.analyticService,
                    sdkConfiguration
                ) {
                    publishNextInQueue()
                }
            )
        }

        // Register the impression on the backend
        payload.get("impression_url")?.asString?.let {
            dataClient.registerImpression(it)
        }

        super.onClientMessageEvent(msgHolder.messagingClient, msgHolder.clientMessage)
    }
}

enum class WidgetType(val event: String) {
    TEXT_PREDICTION("text-prediction-created"),
    TEXT_PREDICTION_FOLLOW_UP("text-prediction-follow-up-updated"),
    IMAGE_PREDICTION("image-prediction-created"),
    IMAGE_PREDICTION_FOLLOW_UP("image-prediction-follow-up-updated"),
    TEXT_QUIZ("text-quiz-created"),
    IMAGE_QUIZ("image-quiz-created"),
    TEXT_POLL("text-poll-created"),
    IMAGE_POLL("image-poll-created"),
    POINTS_TUTORIAL("points-tutorial"),
    ALERT("alert-created");

    companion object {
        private val map = values().associateBy(WidgetType::event)
        fun fromString(type: String) = map[type]
    }
}

internal interface WidgetDataClient {
    suspend fun voteAsync(widgetVotingUrl: String, voteId: String)
    fun registerImpression(impressionUrl: String)
    suspend fun rewardAsync(rewardUrl: String, analyticsService: AnalyticsService): Reward?
}

internal fun MessagingClient.asWidgetManager(
    dataClient: WidgetDataClient,
    widgetInfosStream: SubscriptionManager<SpecifiedWidgetView?>,
    context: Context,
    session: LiveLikeContentSession,
    sdkConfiguration: EngagementSDK.SdkConfiguration
): WidgetManager {
    return WidgetManager(this, dataClient, widgetInfosStream, context, session, sdkConfiguration)
}
