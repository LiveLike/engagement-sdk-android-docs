package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.util.AttributeSet
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.getTotalPoints
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.pointTutorialSeen
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.viewModel.PointTutorialWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetViewModel
import kotlinx.android.synthetic.main.atom_widget_points_tutorial.view.pointsAnimation
import kotlinx.android.synthetic.main.atom_widget_points_tutorial.view.pointsTutoView

class PointsTutorialView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {
    private var viewModel: PointTutorialWidgetViewModel? = null
    override var widgetViewModel: WidgetViewModel? = null
        set(value) {
            field = value
            viewModel = value as PointTutorialWidgetViewModel
            viewModel?.apply {
                startDismissTimout(5000) {
                    removeAllViews()
                }
                pointsAnimation.playAnimation()
                pointsTutoView.startAnimation(getTotalPoints())
            }
        }

    override var dismissFunc: ((action: DismissAction) -> Unit)? =
        {
            viewModel?.dismissWidget(it)
            removeAllViews()
        }

    init {
        pointTutorialSeen()
        inflate(context, R.layout.atom_widget_points_tutorial, this)
    }
}
