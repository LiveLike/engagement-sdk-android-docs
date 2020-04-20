package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.AnalyticsWidgetSpecificInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.domain.GamificationManager
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.view.addGamificationAnalyticsData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO inherit all widget viewModels from here and  add widget common code here.
internal abstract class WidgetViewModel<T : Resource>(
    private val onDismiss: () -> Unit,
    val analyticsService: AnalyticsService
) : BaseViewModel() {

    var widgetInfos: WidgetInfos? = null
    var sdkConfiguration: EngagementSDK.SdkConfiguration? = null
    var widgetMessagingClient: WidgetManager? = null
    var programRepository: ProgramRepository? = null
    var userRepository: UserRepository? = null

    constructor(
        widgetInfos: WidgetInfos,
        sdkConfiguration: EngagementSDK.SdkConfiguration,
        userRepository: UserRepository,
        programRepository: ProgramRepository? = null,
        widgetMessagingClient: WidgetManager? = null,
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

    val data: SubscriptionManager<T> =
        SubscriptionManager()
    val results: SubscriptionManager<T> =
        SubscriptionManager()

    val state: Stream<WidgetState> =
        SubscriptionManager()
    val currentVote: SubscriptionManager<String?> =
        SubscriptionManager()

    val gamificationProfile: Stream<ProgramGamificationProfile>
        get() = programRepository?.programGamificationProfileStream ?: SubscriptionManager()
    val rewardsType: RewardsType
        get() = programRepository?.rewardType ?: RewardsType.NONE

    var animationEggTimerProgress = 0f

    var currentWidgetId: String = ""
    var currentWidgetType: WidgetType? = null

    val interactionData = AnalyticsWidgetInteractionInfo()
    val widgetSpecificInfo = AnalyticsWidgetSpecificInfo()

    protected open fun confirmInteraction() {
        currentWidgetType?.let {
            analyticsService.trackWidgetInteraction(
                it.toAnalyticsString(),
                currentWidgetId,
                interactionData
            )
        }
        uiScope.launch {
            data.currentData?.rewards_url?.let {
                userRepository?.getGamificationReward(it, analyticsService)?.let { pts ->
                    programRepository?.programGamificationProfileStream?.onNext(pts)
                    widgetMessagingClient?.let { widgetMessagingClient ->
                        GamificationManager.checkForNewBadgeEarned(pts, widgetMessagingClient)
                    }
                    interactionData.addGamificationAnalyticsData(pts)
                }
            }
            delay(2000)
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
                if (currentVote.latest() == null) {
                    dismissWidget(DismissAction.TIMEOUT)
                    function?.invoke()
                } else {
                    state.onNext(WidgetState.LOCK_INTERACTION)
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

enum class WidgetState {
    LOCK_INTERACTION, // It is to indicate current interaction is done.
    SHOW_RESULTS, // It is to tell view to show results
    SHOW_GAMIFICATION,
    DISMISS
}
