package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.data.models.Badge
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.viewModel.CollectBadgeWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import kotlinx.android.synthetic.main.widget_gamification_collect_badge.view.badge_iv
import kotlinx.android.synthetic.main.widget_gamification_collect_badge.view.badge_name_tv
import kotlinx.android.synthetic.main.widget_gamification_collect_badge.view.collect_badge_button

class CollectBadgeWidgetView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {
    private var viewModel: CollectBadgeWidgetViewModel? = null
    override var widgetViewModel: ViewModel? = null
        set(value) {
            field = value
            viewModel = value as CollectBadgeWidgetViewModel
            viewModel?.run {
                startDismissTimeout(5000) {
                    this.animationEventsStream.onNext(ViewAnimationEvents.BADGE_COLLECTED)
                    removeAllViews()
                }
                animateView(badge)
            }
        }

    private fun animateView(badge: Badge) {
        badge_iv.loadImage(badge.imageFile, AndroidResource.dpToPx(80))
        badge_name_tv.text = badge.name

        collect_badge_button.setOnClickListener {
            viewModel?.let {
                it.analyticsService.trackBadgeCollectedButtonPressed(it.badge.id, it.badge.level)
                it.animationEventsStream.onNext(ViewAnimationEvents.BADGE_COLLECTED)
            }
            dismissFunc?.invoke(DismissAction.TIMEOUT)
        }
    }

    override var dismissFunc: ((action: DismissAction) -> Unit)? =
        {
            viewModel?.dismissWidget(it)
            removeAllViews()
        }

    init {
        inflate(context, R.layout.widget_gamification_collect_badge, this)
    }
}
