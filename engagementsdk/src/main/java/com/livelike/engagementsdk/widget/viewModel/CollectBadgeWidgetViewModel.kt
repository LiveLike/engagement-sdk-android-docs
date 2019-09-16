package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.data.models.Badge

internal class CollectBadgeWidgetViewModel(
    val badge: Badge,
    onDismiss: () -> Unit,
    analyticsService: AnalyticsService
) : WidgetViewModel(onDismiss, analyticsService) {

    override fun dismissWidget(action: DismissAction) {
        super.dismissWidget(action)
    }
}
