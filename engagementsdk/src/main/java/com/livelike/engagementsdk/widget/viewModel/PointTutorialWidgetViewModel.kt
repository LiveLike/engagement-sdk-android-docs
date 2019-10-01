package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.data.models.RewardsType

internal class PointTutorialWidgetViewModel(
    onDismiss: () -> Unit,
    analyticsService: AnalyticsService,
    val rewardType: RewardsType,
    val programGamificationProfile: ProgramGamificationProfile?
) : WidgetViewModel(onDismiss, analyticsService) {

    override fun dismissWidget(action: DismissAction) {
        super.dismissWidget(action)
        analyticsService.trackPointTutorialSeen(action.name, 5000L)
    }
}
