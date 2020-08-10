package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.ViewAnimationEvents
<<<<<<< Updated upstream
import com.livelike.engagementsdk.widget.data.models.Badge
import com.livelike.engagementsdk.core.utils.SubscriptionManager
=======
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.widget.data.models.Badge
>>>>>>> Stashed changes
import com.livelike.engagementsdk.widget.model.Resource

internal class CollectBadgeWidgetViewModel(
    val badge: Badge,
    onDismiss: () -> Unit,
    analyticsService: AnalyticsService,
    val animationEventsStream: SubscriptionManager<ViewAnimationEvents>
) : WidgetViewModel<Resource>(onDismiss, analyticsService) {

    override fun dismissWidget(action: DismissAction) {
        animationEventsStream.onNext(ViewAnimationEvents.BADGE_COLLECTED)
        super.dismissWidget(action)
    }

    override fun vote(value: String) {
    }
}
