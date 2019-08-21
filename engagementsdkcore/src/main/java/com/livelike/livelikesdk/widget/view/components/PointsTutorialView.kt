package com.livelike.livelikesdk.widget.view.components

import android.content.Context
import android.util.AttributeSet
import com.livelike.engagementsdkapi.DismissAction
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getTotalPoints
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import com.livelike.livelikesdk.widget.viewModel.PointTutorialWidgetViewModel
import com.livelike.livelikesdk.widget.viewModel.WidgetViewModel
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
        inflate(context, R.layout.atom_widget_points_tutorial, this)
    }
}