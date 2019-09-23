package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.data.models.Badge
import com.livelike.engagementsdk.utils.SubscriptionManager

internal class CollectBadgeWidgetViewModel(
    val badge: Badge,
    onDismiss: () -> Unit,
    analyticsService: AnalyticsService,
    val animationEventsStream: SubscriptionManager<ViewAnimationEvents>
) : WidgetViewModel(onDismiss, analyticsService) {

    override fun dismissWidget(action: DismissAction) {
        super.dismissWidget(action)
    }
}
