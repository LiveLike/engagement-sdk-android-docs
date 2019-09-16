package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction

internal class PointTutorialWidgetViewModel(
    onDismiss: () -> Unit,
    analyticsService: AnalyticsService
) : WidgetViewModel(onDismiss, analyticsService) {

    override fun dismissWidget(action: DismissAction) {
        super.dismissWidget(action)
        analyticsService.trackPointTutorialSeen(action.name, 5000L)
    }
}
