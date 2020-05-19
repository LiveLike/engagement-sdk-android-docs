package com.livelike.engagementsdk.widget.viewModel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v7.widget.RecyclerView
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.core.services.messaging.Error
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.debounce
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logVerbose
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.domain.GamificationManager
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.engagementsdk.widget.services.network.WidgetDataClient
import com.livelike.engagementsdk.widget.services.network.WidgetDataClientImpl
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.view.addGamificationAnalyticsData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class QuizWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class QuizViewModel(
    widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    sdkConfiguration: EngagementSDK.SdkConfiguration,
    val context: Context,
    var onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository ? = null,
    val widgetMessagingClient: WidgetManager ? = null
) : BaseViewModel() {
    var points: Int? = null
    val gamificationProfile: Stream<ProgramGamificationProfile>
        get() = programRepository?.programGamificationProfileStream ?: SubscriptionManager()
    val rewardsType: RewardsType
        get() = programRepository?.rewardType ?: RewardsType.NONE
    val data: SubscriptionManager<QuizWidget> =
        SubscriptionManager()
    val results: Stream<Resource> =
        SubscriptionManager()
    val currentVoteId: SubscriptionManager<String?> =
        SubscriptionManager()
    private val debouncedVoteId = currentVoteId.debounce()
    private val dataClient: WidgetDataClient = WidgetDataClientImpl()
//    var state: Stream<String> =
//        SubscriptionManager() // results

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
            pubnub = PubnubMessagingClient.getInstance(it, userRepository.currentUserStream.latest()?.id)
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
        logDebug { "Quiz Widget selectedPosition:${adapter?.selectedPosition}" }
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) return // Nothing has been clicked

        uiScope.launch {
            adapter?.apply {
                val url = myDataset[selectedPosition].getMergedVoteUrl()
                url?.let { dataClient.voteAsync(url, myDataset[selectedPosition].id, userRepository.userAccessToken) }
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

    fun startDismissTimout(
        timeout: String,
        widgetViewThemeAttributes: WidgetViewThemeAttributes
    ) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                debouncedVoteId.unsubscribe(javaClass)
                adapter?.selectionLocked = true
//                state.onNext(WidgetState.LOCK_INTERACTION.name)
                vote()
                delay(500)
                widgetState.onNext(WidgetStates.RESULTS)
                resultsState(widgetViewThemeAttributes)
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
        widgetState.onNext(WidgetStates.FINISHED)
        logDebug { "dismiss Quiz Widget, reason:${action.name}" }
        onDismiss()
        cleanUp()
        viewModelJob.cancel()
    }

    private fun resultsState(widgetViewThemeAttributes: WidgetViewThemeAttributes) {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            dismissWidget(DismissAction.TIMEOUT)
            return
        }

        val isUserCorrect = adapter?.selectedPosition?.let { adapter?.myDataset?.get(it)?.is_correct } ?: false
        adapter?.selectionLocked = true
        logDebug { "Quiz View ,showing result isUserCorrect:$isUserCorrect" }
        println("QuizViewModel.resultsState->$animationPath")
        uiScope.launch {
            data.currentData?.resource?.rewards_url?.let {
                userRepository.getGamificationReward(it, analyticsService)?.let { pts ->
                    programRepository?.programGamificationProfileStream?.onNext(pts)
                    points = pts.newPoints
                    widgetMessagingClient?.let { widgetMessagingClient ->
                        GamificationManager.checkForNewBadgeEarned(pts, widgetMessagingClient)
                    }
                    interactionData.addGamificationAnalyticsData(pts)
                }
            }
//            state.onNext("results")
            currentWidgetType?.let { analyticsService.trackWidgetInteraction(it.toAnalyticsString(), currentWidgetId, interactionData) }
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
//        state.onNext(null)
        animationEggTimerProgress = 0f

        currentWidgetType = null
        currentWidgetId = ""
        interactionData.reset()
    }

    fun onOptionClicked() {
        interactionData.incrementInteraction()
    }
}
