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
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO inherit all widget viewModels from here and  add widget common code here.
internal abstract class WidgetViewModel(
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

    private var timeoutStarted = false

    val gamificationProfile: Stream<ProgramGamificationProfile>?
        get() = programRepository?.programGamificationProfileStream
    val rewardsType: RewardsType?
        get() = programRepository?.rewardType

    var animationEggTimerProgress = 0f

    var currentWidgetId: String = ""
    var currentWidgetType: WidgetType? = null

    val interactionData = AnalyticsWidgetInteractionInfo()
    val widgetSpecificInfo = AnalyticsWidgetSpecificInfo()

    open fun dismissWidget(action: DismissAction) {
        onDismiss()
        onClear()
    }

    fun startDismissTimeout(timeout: Long, function: () -> Unit) {
        if (!timeoutStarted) {
            timeoutStarted = true
            uiScope.launch {
                delay(timeout)
                dismissWidget(DismissAction.TIMEOUT)
                function()
                timeoutStarted = false
            }
        }
    }

    open fun onClear() {
        viewModelJob.cancel()
        timeoutStarted = false
    }
}
