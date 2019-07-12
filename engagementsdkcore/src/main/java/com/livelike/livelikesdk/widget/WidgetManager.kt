package com.livelike.livelikesdk.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdkapi.Stream
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.services.analytics.AnalyticsService
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.services.messaging.proxies.MessagingClientProxy
import com.livelike.livelikesdk.utils.SubscriptionManager

internal class WidgetManager(
    upstream: MessagingClient,
    private val dataClient: WidgetDataClient,
    private val currentWidgetViewStream: Stream<SpecifiedWidgetView?>,
    private val context: Context,
    private val analyticsService: AnalyticsService,
    private val sdkConfiguration : LiveLikeSDK.SdkConfiguration
) :
    MessagingClientProxy(upstream) {

    val handler = Handler(Looper.getMainLooper())

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        val widgetType = event.message.get("event").asString ?: ""
        val payload = event.message["payload"].asJsonObject
        val widgetId = payload["id"].asString

        // Filter only valid widget types here
        if (WidgetType.fromString(widgetType) != null) {
            handler.post {
                currentWidgetViewStream.onNext(null)
                currentWidgetViewStream.onNext(
                    WidgetProvider().get(
                        WidgetInfos(widgetType, payload, widgetId),
                        context,
                        {
                            currentWidgetViewStream.onNext(null)
                        },
                        analyticsService,
                        sdkConfiguration
                    )
                )
            }


            // Register the impression on the backend
            payload.get("impression_url")?.asString?.let {
                dataClient.registerImpression(it)
            }

            super.onClientMessageEvent(client, event)
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
    ALERT("alert-created");

    companion object {
        private val map = values().associateBy(WidgetType::event)
        fun fromString(type: String) = map[type]
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
    widgetInfosStream: SubscriptionManager<SpecifiedWidgetView?>,
    context: Context,
    analyticsService : AnalyticsService,
    sdkConfiguration : LiveLikeSDK.SdkConfiguration
): WidgetManager {
    return WidgetManager(this, dataClient, widgetInfosStream, context, analyticsService, sdkConfiguration)
}
