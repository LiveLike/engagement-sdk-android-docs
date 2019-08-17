package com.livelike.livelikesdk.widget.viewModel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v7.widget.RecyclerView
import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdkapi.DismissAction
import com.livelike.livelikesdk.EngagementSDK
import com.livelike.livelikesdk.Stream
import com.livelike.livelikesdk.WidgetInfos
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
import com.livelike.livelikesdk.utils.logVerbose
import com.livelike.livelikesdk.utils.toAnalyticsString
import com.livelike.livelikesdk.widget.WidgetDataClient
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.model.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class QuizWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class QuizViewModel(widgetInfos: WidgetInfos, private val analyticsService: AnalyticsService, sdkConfiguration: EngagementSDK.SdkConfiguration, val context: Context) : WidgetViewModel() {
    var points: Int? = null
    val data: SubscriptionManager<QuizWidget> = SubscriptionManager()
    val results: Stream<Resource> = SubscriptionManager()
    val currentVoteId: SubscriptionManager<String?> = SubscriptionManager()
    private val debouncedVoteId = currentVoteId.debounce()
    private val dataClient: WidgetDataClient = EngagementDataClientImpl()
    var state: Stream<String> = SubscriptionManager() // results

    var adapter: WidgetOptionsViewAdapter? = null
    private var timeoutStarted = false
    var animationProgress = 0f
    internal var animationPath = ""
    var voteUrl: String? = null
    private var pubnub: PubnubMessagingClient? = null
    var animationEggTimerProgress = 0f

    private var currentWidgetId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()

    init {
        sdkConfiguration.pubNubKey.let {
            pubnub = PubnubMessagingClient(it)
            pubnub?.addMessagingEventListener(object : MessagingEventListener {
                override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
                    val widgetType = event.message.get("event").asString ?: ""
                    logVerbose { "type is : $widgetType" }
                    val payload = event.message["payload"].asJsonObject
                    Handler(Looper.getMainLooper()).post {
                        results.onNext(gson.fromJson(payload.toString(), Resource::class.java) ?: null)
                    }
                }

                override fun onClientMessageError(client: MessagingClient, error: Error) {
                }
                override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {
                }
            })
        }
        debouncedVoteId.subscribe(javaClass) {
            if (it != null) {
                vote()
            }
        }

        widgetObserver(widgetInfos)
    }

    private fun vote() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) return // Nothing has been clicked

        uiScope.launch {
            adapter?.apply {
                val url = myDataset[selectedPosition].getMergedVoteUrl()
                url?.let { dataClient.voteAsync(url, myDataset[selectedPosition].id) }
            }
            adapter?.notifyDataSetChanged()
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null &&
            (WidgetType.fromString(widgetInfos.type) == WidgetType.IMAGE_QUIZ ||
                    WidgetType.fromString(widgetInfos.type) == WidgetType.TEXT_QUIZ)
        ) {
            val resource = gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
            resource?.apply {
                pubnub?.subscribe(listOf(resource.subscribe_channel))
                data.onNext(WidgetType.fromString(widgetInfos.type)?.let { QuizWidget(it, resource) })
            }
            currentWidgetId = widgetInfos.widgetId
            currentWidgetType = WidgetType.fromString(widgetInfos.type)
            interactionData.widgetDisplayed()
        } else {
            cleanUp()
        }
    }

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                debouncedVoteId.unsubscribe(javaClass)
                adapter?.selectionLocked = true
                vote()
                delay(2000)
                resultsState()
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
        viewModelJob.cancel()
    }

    private fun resultsState() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            dismissWidget(DismissAction.TIMEOUT)
            return
        }

        val isUserCorrect = adapter?.selectedPosition?.let { adapter?.myDataset?.get(it)?.is_correct } ?: false
        val rootPath = if (isUserCorrect) "correctAnswer" else "wrongAnswer"
        animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, context) ?: ""

        adapter?.selectionLocked = true

        uiScope.launch {
            data.currentData?.resource?.rewards_url?.let {
                points = dataClient.rewardAsync(it, analyticsService)?.new_points
                interactionData.pointEarned = points ?: 0
            }

            state.onNext("results")
            currentWidgetType?.let { analyticsService.trackWidgetInteraction(it.toAnalyticsString(), currentWidgetId, interactionData) }
            delay(6000)
            dismissWidget(DismissAction.TIMEOUT)
        }
    }

    private fun cleanUp() {
        vote() // Vote on dismiss
        pubnub?.unsubscribeAll()
        timeoutStarted = false
        adapter = null
        animationProgress = 0f
        animationPath = ""
        voteUrl = null
        data.onNext(null)
        results.onNext(null)
        state.onNext(null)
        animationEggTimerProgress = 0f

        currentWidgetType = null
        currentWidgetId = ""
        interactionData.reset()
    }

    fun onOptionClicked() {
        interactionData.incrementInteraction()
    }
}
