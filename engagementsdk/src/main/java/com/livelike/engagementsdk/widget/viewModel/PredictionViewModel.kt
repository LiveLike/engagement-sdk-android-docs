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
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.addWidgetPredictionVoted
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.view.addGamificationAnalyticsData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PredictionWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class PredictionViewModel(
    widgetInfos: WidgetInfos,
    private val appContext: Context,
    private val analyticsService: AnalyticsService,
    sdkConfiguration: EngagementSDK.SdkConfiguration,
    val onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository? = null,
    val widgetMessagingClient: WidgetManager? = null
) : BaseViewModel() {
    var followUp: Boolean = false
    var points: Int? = null
    val gamificationProfile: Stream<ProgramGamificationProfile>
        get() = programRepository?.programGamificationProfileStream ?: SubscriptionManager()
    val rewardsType: RewardsType
        get() = programRepository?.rewardType ?: RewardsType.NONE
    val data: SubscriptionManager<PredictionWidget?> =
        SubscriptionManager()
    private val dataClient: WidgetDataClient = WidgetDataClientImpl()
//    var state: Stream<String?> =
//        SubscriptionManager() // confirmation, followup
    var results: Stream<Resource> =
        SubscriptionManager()
    var adapter: WidgetOptionsViewAdapter? = null
    var timeoutStarted = false
    var animationProgress = 0f
    var animationEggTimerProgress = 0f
    var animationPath = ""
    private var pubnub: PubnubMessagingClient? = null

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
                        results.onNext(
                            gson.fromJson(payload.toString(), Resource::class.java) ?: null
                        )
                    }
                }

                override fun onClientMessageError(client: MessagingClient, error: Error) {}
                override fun onClientMessageStatus(
                    client: MessagingClient,
                    status: ConnectionStatus
                ) {
                }
            })
        }
        widgetObserver(widgetInfos)
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        cleanUp()
        if (widgetInfos != null) {
            val type = WidgetType.fromString(widgetInfos.type)
            if (type == WidgetType.IMAGE_PREDICTION ||
                type == WidgetType.IMAGE_PREDICTION_FOLLOW_UP ||
                type == WidgetType.TEXT_PREDICTION ||
                type == WidgetType.TEXT_PREDICTION_FOLLOW_UP
            ) {
                val resource = gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
                resource?.apply {
                    pubnub?.subscribe(listOf(resource.subscribe_channel))
                    data.onNext(PredictionWidget(type, resource))
                    widgetState.onNext(WidgetStates.READY)
                }

                currentWidgetId = widgetInfos.widgetId
                currentWidgetType = type
                interactionData.widgetDisplayed()
            }
        } else {
            data.onNext(null)
        }
    }

    private val runnable = Runnable { }

    // TODO: need to move the followup logic back to the widget observer instead of there
    fun startDismissTimout(
        timeout: String,
        isFollowup: Boolean,
        widgetViewThemeAttributes: WidgetViewThemeAttributes
    ) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            if (isFollowup) {
                uiScope.launch {
                    delay(AndroidResource.parseDuration(timeout))
                    dismissWidget(DismissAction.TIMEOUT)
                }
                data.currentData?.apply {
                    val selectedPredictionId = getWidgetPredictionVotedAnswerIdOrEmpty(if (resource.text_prediction_id.isNullOrEmpty()) resource.image_prediction_id else resource.text_prediction_id)
                    uiScope.launch {
                        delay(if (selectedPredictionId.isNotEmpty()) AndroidResource.parseDuration(timeout) else 0)
                        dismissWidget(DismissAction.TIMEOUT)
                    }
                }
            } else {
                uiScope.launch {
                    delay(AndroidResource.parseDuration(timeout))
                    confirmationState(widgetViewThemeAttributes)
                }
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
        logDebug { "dismiss Prediction Widget, reason:${action.name}" }
        onDismiss()
        cleanUp()
    }

    fun onOptionClicked() {
        uiScope.launch {
            vote()
        }
        interactionData.incrementInteraction()
    }

    internal fun followupState(
        selectedPredictionId: String,
        correctOptionId: String,
        widgetViewThemeAttributes: WidgetViewThemeAttributes
    ) {
        if (followUp)
            return
        followUp = true
        adapter?.correctOptionId = correctOptionId
        adapter?.userSelectedOptionId = selectedPredictionId
        adapter?.selectionLocked = true

        data.onNext(data.currentData?.apply {
            resource.getMergedOptions()?.forEach { opt ->
                opt.percentage = opt.getPercent(resource.getMergedTotal().toFloat())
            }
        })

        val isUserCorrect = adapter?.myDataset?.find { it.id == selectedPredictionId }?.is_correct ?: false
        val rootPath = if (isUserCorrect) widgetViewThemeAttributes.widgetWinAnimation else widgetViewThemeAttributes.widgetLoseAnimation
        animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, appContext) ?: ""
        uiScope.launch {
            data.currentData?.resource?.rewards_url?.let {
                userRepository.getGamificationReward(it, analyticsService)?.let { pts ->
                    programRepository?.programGamificationProfileStream?.onNext(pts)
                    points = pts.newPoints
                    widgetMessagingClient?.let { widgetMessagingClient ->
                        GamificationManager.checkForNewBadgeEarned(pts, widgetMessagingClient)
                    }
                    interactionData.pointEarned = points ?: 0
                }
            }
//            state.onNext("followup")
            widgetState.onNext(WidgetStates.RESULTS)
        }
        logDebug { "Prediction Widget Follow Up isUserCorrect:$isUserCorrect" }
    }

    private fun confirmationState(widgetViewThemeAttributes: WidgetViewThemeAttributes) {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            dismissWidget(DismissAction.TIMEOUT)
            return
        }

        adapter?.selectionLocked = true
        val rootPath = widgetViewThemeAttributes.stayTunedAnimation
        animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, appContext) ?: ""
        logDebug { "Prediction Widget selected Position:${adapter?.selectedPosition}" }
        uiScope.launch {
            vote()
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
            pubnub?.stop()
            pubnub?.unsubscribeAll()
//            state.onNext("confirmation")
            widgetState.onNext(WidgetStates.RESULTS)
            currentWidgetType?.let { analyticsService.trackWidgetInteraction(it.toAnalyticsString(), currentWidgetId, interactionData) }
            delay(3000)
            dismissWidget(DismissAction.TIMEOUT)
        }
    }

    private fun cleanUp() {
        uiScope.launch {
            vote()
        }
        timeoutStarted = false
        adapter = null
        animationProgress = 0f
        animationPath = ""
//        state.onNext("")
        data.onNext(null)
        animationEggTimerProgress = 0f
        currentWidgetType = null
        currentWidgetId = ""
        interactionData.reset()
    }

    @Suppress("USELESS_ELVIS")
    private suspend fun vote() {
        data.currentData?.let {
            adapter?.apply {
                if (selectedPosition != RecyclerView.NO_POSITION) { // User has selected an option
                    val selectedOption = it.resource.getMergedOptions()?.get(selectedPosition)

                    // Prediction widget votes on dismiss
                    selectedOption?.getMergedVoteUrl()?.let { url ->
                        dataClient.voteAsync(url, selectedOption.id, userRepository.userAccessToken)
                    }

                    // Save widget id and voted option for followup widget
                    addWidgetPredictionVoted(it.resource.id ?: "", selectedOption?.id ?: "")
                }
            }
        }
    }
}
