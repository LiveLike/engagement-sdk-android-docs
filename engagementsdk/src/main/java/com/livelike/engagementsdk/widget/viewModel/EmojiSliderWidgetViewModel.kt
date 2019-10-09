package com.livelike.engagementsdk.widget.viewModel

import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.data.repository.UserRepository
import com.livelike.engagementsdk.domain.GamificationManager
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.services.messaging.Error
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.engagementsdk.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.services.network.WidgetDataClient
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.debounce
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.logDebug
import com.livelike.engagementsdk.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.ImageSliderEntity
import com.livelike.engagementsdk.widget.view.addGamificationAnalyticsData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.FormBody

internal class EmojiSliderWidgetViewModel(
    widgetInfos: WidgetInfos,
    analyticsService: AnalyticsService,
    sdkConfiguration: EngagementSDK.SdkConfiguration,
    onDismiss: () -> Unit,
    userRepository: UserRepository,
    programRepository: ProgramRepository,
    widgetMessagingClient: WidgetManager
) : WidgetViewModel(widgetInfos, sdkConfiguration, userRepository, programRepository, widgetMessagingClient, onDismiss, analyticsService) {

    val data: SubscriptionManager<ImageSliderEntity> = SubscriptionManager()
    val results: SubscriptionManager<ImageSliderEntity> = SubscriptionManager()
    val currentVote: SubscriptionManager<String?> = SubscriptionManager()
    private val debouncer = currentVote.debounce()
    private val dataClient: WidgetDataClient = EngagementDataClientImpl()

    var animationResultsProgress = 0f
    private var animationPath = ""

    var voteUrl: String? = null

    private var pubnub: PubnubMessagingClient? = null

    init {
        sdkConfiguration.pubNubKey.let {
            pubnub = PubnubMessagingClient(it)
            pubnub?.addMessagingEventListener(object : MessagingEventListener {
                override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
                    val widgetType = event.message.get("event").asString ?: ""
                    logDebug { "type is : $widgetType" }
                    val payload = event.message["payload"].asJsonObject
                    Handler(Looper.getMainLooper()).post {
                        results.onNext(gson.fromJson(payload.toString(), ImageSliderEntity::class.java) ?: null)
                    }
                }

                override fun onClientMessageError(client: MessagingClient, error: Error) {}
                override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {}
            })
        }

        debouncer.subscribe(javaClass.simpleName) {
            if (it != null) vote(it)
        }
        widgetObserver(widgetInfos)
    }

    private fun vote(value: String) {
        uiScope.launch {
            data.latest()?.voteUrl?.let {
                dataClient.voteAsync(it, "", userRepository?.userAccessToken, FormBody.Builder()
                    .add("magnitude", value).build())
            }
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos) {
            val resource = gson.fromJson(widgetInfos.payload.toString(), ImageSliderEntity::class.java) ?: null
            resource?.apply {
                pubnub?.subscribe(listOf(resource.subscribe_channel))
                data.onNext(resource)
            }
            currentWidgetId = widgetInfos.widgetId
            currentWidgetType = WidgetType.fromString(widgetInfos.type)
            interactionData.widgetDisplayed()
    }

    private fun confirmationState() {
        uiScope.launch {
            data.currentData?.rewards_url?.let {
                userRepository?.getGamificationReward(it, analyticsService)?.let { pts ->
                    programRepository?.programGamificationProfileStream?.onNext(pts)
                    widgetMessagingClient?.let { GamificationManager.checkForNewBadgeEarned(pts, it) }
                    interactionData.addGamificationAnalyticsData(pts)
                }
            }

            currentWidgetType?.let { analyticsService.trackWidgetInteraction(it.toAnalyticsString(), currentWidgetId, interactionData) }
            delay(6000)
            dismissWidget(DismissAction.TIMEOUT)
        }
    }

    override fun dismissWidget(action: DismissAction) {
        super.dismissWidget(action)
    }

    override fun onClear() {
        super.onClear()
    }
}
