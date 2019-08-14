package com.livelike.livelikesdk.widget.viewModel

import android.os.Handler
import android.os.Looper
import android.support.v7.widget.RecyclerView
import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdkapi.AnalyticsWidgetSpecificInfo
import com.livelike.engagementsdkapi.DismissAction
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.EngagementSDK
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.ConnectionStatus
import com.livelike.livelikesdk.services.messaging.Error
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.services.messaging.MessagingEventListener
import com.livelike.livelikesdk.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.livelikesdk.services.network.EngagementDataClientImpl
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.utils.debounce
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.utils.logDebug
import com.livelike.livelikesdk.utils.toAnalyticsString
import com.livelike.livelikesdk.widget.WidgetDataClient
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.model.Resource
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PollWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class PollViewModel(widgetInfos: WidgetInfos, private val analyticsService: AnalyticsService, sdkConfiguration: EngagementSDK.SdkConfiguration) : WidgetViewModel() {
    var points: SubscriptionManager<Int?> = SubscriptionManager()
    val data: SubscriptionManager<PollWidget> = SubscriptionManager()
    val results: SubscriptionManager<Resource> = SubscriptionManager()
    val currentVoteId: SubscriptionManager<String?> = SubscriptionManager()
    private val debouncer = currentVoteId.debounce()
    private val dataClient: WidgetDataClient = EngagementDataClientImpl()

    var adapter: WidgetOptionsViewAdapter? = null
    var timeoutStarted = false
    var animationResultsProgress = 0f
    private var animationPath = ""
    var voteUrl: String? = null
    private var pubnub: PubnubMessagingClient? = null
    var animationEggTimerProgress = 0f
    private var currentWidgetId: String = ""
    private var currentWidgetType: WidgetType? = null

    private val interactionData = AnalyticsWidgetInteractionInfo()
    private val widgetSpecificInfo = AnalyticsWidgetSpecificInfo()

    init {
        sdkConfiguration.pubNubKey.let {
            pubnub = PubnubMessagingClient(it)
            pubnub?.addMessagingEventListener(object : MessagingEventListener {
                override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
                    val widgetType = event.message.get("event").asString ?: ""
                    logDebug { "type is : $widgetType" }
                    val payload = event.message["payload"].asJsonObject
                    Handler(Looper.getMainLooper()).post {
                        results.onNext(gson.fromJson(payload.toString(), Resource::class.java) ?: null)
                    }
                }

                override fun onClientMessageError(client: MessagingClient, error: Error) {}
                override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {}
            })
        }

        debouncer.subscribe(javaClass.simpleName) {
            if (it != null) vote()
        }

        widgetObserver(widgetInfos)
    }

    private fun vote() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) return // Nothing has been clicked

        uiScope.launch {
                adapter?.apply {
                    val url = myDataset[selectedPosition].getMergedVoteUrl()
                    url?.let { dataClient.voteAsync(it, myDataset[selectedPosition].id) }
                }
                adapter?.showPercentage = true
                adapter?.notifyDataSetChanged()
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null &&
            (WidgetType.fromString(widgetInfos.type) == WidgetType.TEXT_POLL ||
                    WidgetType.fromString(widgetInfos.type) == WidgetType.IMAGE_POLL)
        ) {
            val resource = gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
            resource?.apply {
                pubnub?.subscribe(listOf(resource.subscribe_channel))
                data.onNext(WidgetType.fromString(widgetInfos.type)?.let { PollWidget(it, resource) })
            }
            currentWidgetId = widgetInfos.widgetId
            currentWidgetType = WidgetType.fromString(widgetInfos.type)
            interactionData.widgetDisplayed()
        }
    }

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                confirmationState()
            }
        }
    }

    fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            analyticsService.trackWidgetDismiss(
                it.toAnalyticsString(),
                currentWidgetId,
                interactionData,
                adapter?.selectionLocked,
                action
            )
        }
        cleanUp()
    }

    private fun confirmationState() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            dismissWidget(DismissAction.TIMEOUT)
            return
        }

        adapter?.selectionLocked = true

        currentWidgetType?.let { analyticsService.trackWidgetInteraction(it.toAnalyticsString(), currentWidgetId, interactionData) }

        uiScope.launch {
            data.currentData?.resource?.rewards_url?.let {
                points.onNext(dataClient.rewardAsync(it)?.new_points)
                interactionData.pointEarned = points.currentData ?: 0
            }
            delay(6000)
            dismissWidget(DismissAction.TIMEOUT)
        }
    }

    private fun cleanUp() {
        vote() // Vote on dismiss
        pubnub?.unsubscribeAll()
        timeoutStarted = false
        adapter = null
        animationResultsProgress = 0f
        animationPath = ""
        voteUrl = null
        data.onNext(null)
        results.onNext(null)
        animationEggTimerProgress = 0f
        currentVoteId.onNext(null)

        interactionData.reset()
        widgetSpecificInfo.reset()
        currentWidgetId = ""
        currentWidgetType = null
        viewModelJob.cancel("Widget Cleanup")
    }

    var firstClick = true

    fun onOptionClicked() {
        if (firstClick) {
            firstClick = false
        }
        interactionData.incrementInteraction()
    }
}
