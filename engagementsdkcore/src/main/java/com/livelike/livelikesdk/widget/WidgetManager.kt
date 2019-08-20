package com.livelike.livelikesdk.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.livelikesdk.EngagementSDK
import com.livelike.livelikesdk.Stream
import com.livelike.livelikesdk.WidgetInfos
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.services.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.widget.model.Reward
import java.util.PriorityQueue
import java.util.Queue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class WidgetManager(
    upstream: MessagingClient,
    private val dataClient: WidgetDataClient,
    private val currentWidgetViewStream: Stream<SpecifiedWidgetView?>,
    private val context: Context,
    private val analyticsService: AnalyticsService,
    private val sdkConfiguration: EngagementSDK.SdkConfiguration
) :
    MessagingClientProxy(upstream) {

    data class MessageHolder(val messagingClient: MessagingClient, val clientMessage: ClientMessage) : Comparable<MessageHolder> {
        override fun compareTo(other: MessageHolder): Int {
            return this.clientMessage.message.get("event").asString.length - other.clientMessage.message.get("event").asString.length
        }
    }

    private val messageQueue: Queue<MessageHolder> = PriorityQueue()
    private var activelyCheckForNewElements = true

    init {
        GlobalScope.launch {
            while (true) {
                delay(1000)
                if (activelyCheckForNewElements) {
                    publishNextInQueue()
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

    val handler = Handler(Looper.getMainLooper())

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        messageQueue.add(MessageHolder(client, event))
    }

    private fun publishNextInQueue() {
        if (messageQueue.isNotEmpty()) {
            onDequeue(messageQueue.remove())
        } else {
            activelyCheckForNewElements = true
        }
    }

    private fun onDequeue(msgHolder: MessageHolder) {
        val widgetType = msgHolder.clientMessage.message.get("event").asString ?: ""
        val payload = msgHolder.clientMessage.message["payload"].asJsonObject
        val widgetId = payload["id"].asString

        analyticsService.trackWidgetDisplayed(widgetType, widgetId)

        // Filter only valid widget types here
        if (WidgetType.fromString(widgetType) != null) {
            handler.post {
                currentWidgetViewStream.onNext(
                    WidgetProvider().get(
                        WidgetInfos(widgetType, payload, widgetId),
                        context,
                        analyticsService,
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

            activelyCheckForNewElements = false // Next element will be dequeued on widget dismissal
            super.onClientMessageEvent(msgHolder.messagingClient, msgHolder.clientMessage)
        }
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
    analyticsService: AnalyticsService,
    sdkConfiguration: EngagementSDK.SdkConfiguration
): WidgetManager {
    return WidgetManager(this, dataClient, widgetInfosStream, context, analyticsService, sdkConfiguration)
}
