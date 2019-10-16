package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.AnalyticsWidgetSpecificInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.data.models.RewardsType
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.data.repository.UserRepository
import com.livelike.engagementsdk.domain.GamificationManager
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.debounce
import com.livelike.engagementsdk.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.view.addGamificationAnalyticsData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO inherit all widget viewModels from here and  add widget common code here.
internal abstract class WidgetViewModel<T : Resource>(
    private val onDismiss: () -> Unit,
    val analyticsService: AnalyticsService
) : ViewModel() {

    var widgetInfos: WidgetInfos? = null
    var sdkConfiguration: EngagementSDK.SdkConfiguration? = null
    var widgetMessagingClient: WidgetManager? = null
    var programRepository: ProgramRepository? = null
    var userRepository: UserRepository? = null

    constructor(
        widgetInfos: WidgetInfos,
        sdkConfiguration: EngagementSDK.SdkConfiguration,
        userRepository: UserRepository,
        programRepository: ProgramRepository,
        widgetMessagingClient: WidgetManager,
        onDismiss: () -> Unit,
        analyticsService: AnalyticsService
    ) : this(onDismiss, analyticsService) {
        this.widgetInfos = widgetInfos
        this.sdkConfiguration = sdkConfiguration
        this.userRepository = userRepository
        this.programRepository = programRepository
        this.widgetMessagingClient = widgetMessagingClient
    }

    var timeoutStarted = false


    val data: SubscriptionManager<T> = SubscriptionManager()
    val results: SubscriptionManager<T> = SubscriptionManager()

    val state: Stream<WidgetState> = SubscriptionManager()
    val currentVote: SubscriptionManager<String?> = SubscriptionManager()
    val debouncer = currentVote.debounce()

    val gamificationProfile: Stream<ProgramGamificationProfile>?
        get() = programRepository?.programGamificationProfileStream
    val rewardsType: RewardsType?
        get() = programRepository?.rewardType

    var animationEggTimerProgress = 0f

    var currentWidgetId: String = ""
    var currentWidgetType: WidgetType? = null

    val interactionData = AnalyticsWidgetInteractionInfo()
    val widgetSpecificInfo = AnalyticsWidgetSpecificInfo()


    protected open fun confirmInteraction() {
        currentWidgetType?.let { analyticsService.trackWidgetInteraction(it.toAnalyticsString(), currentWidgetId, interactionData) }
        uiScope.launch {
            data.currentData?.rewards_url?.let {
                userRepository?.getGamificationReward(it, analyticsService)?.let { pts ->
                    programRepository?.programGamificationProfileStream?.onNext(pts)
                    widgetMessagingClient?.let {
                            widgetMessagingClient->GamificationManager.checkForNewBadgeEarned(pts, widgetMessagingClient)
                    }
                    interactionData.addGamificationAnalyticsData(pts)
                }
            }
            if (debouncer.currentData != currentVote.currentData) {
                debouncer.clear()
                currentVote.currentData?.let { value ->
                    vote(value)
                }
            }
            state.onNext(WidgetState.SHOW_RESULTS)
            delay(1000)
            state.onNext(WidgetState.SHOW_GAMIFICATION)
            delay(3000)
            dismissWidget(DismissAction.TIMEOUT)
        }
    }

    abstract fun vote(value: String)

    fun startInteractionTimeout(timeout: Long, function: (() -> Unit)? = null) {
        if (!timeoutStarted) {
            timeoutStarted = true
            uiScope.launch {
                delay(timeout)
                if(currentVote.latest() == null){
                    dismissWidget(DismissAction.TIMEOUT)
                    function?.invoke()
                }else{
                    state.onNext(WidgetState.CONFIRM_INTERACTION)
                    confirmInteraction()
                }
                timeoutStarted = false
            }
        }
    }

    // FYI Right now this Widgetmodel is inherited by tutorial and gamification widgets, so dismiss analytics should be added in more concrete class.
    open fun dismissWidget(action: DismissAction) {
        state.onNext(WidgetState.DISMISS)
        onDismiss()
        onClear()
    }

    open fun onClear() {
        viewModelJob.cancel()
        timeoutStarted = false
    }
}

//Help me! Team contribution is important for better namings.
enum class WidgetState{
    CONFIRM_INTERACTION, // It is to indicate current interaction is done.
    SHOW_RESULTS,      // It is to tell view to show results
    SHOW_GAMIFICATION,
    DISMISS
}

