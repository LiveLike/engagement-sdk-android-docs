package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.data.models.RewardsType
import com.livelike.engagementsdk.widget.model.Resource

internal class PointTutorialWidgetViewModel(
    onDismiss: () -> Unit,
    analyticsService: AnalyticsService,
    val rewardType: RewardsType,
    val programGamificationProfile: ProgramGamificationProfile?
) : WidgetViewModel<Resource>(onDismiss, analyticsService) {

    override fun dismissWidget(action: DismissAction) {
        super.dismissWidget(action)
        analyticsService.trackPointTutorialSeen(action.name, 5000L)
    }

    override fun vote(value: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
