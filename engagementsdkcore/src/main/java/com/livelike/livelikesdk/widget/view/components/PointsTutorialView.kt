package com.livelike.livelikesdk.widget.view.components

import android.content.Context
import android.util.AttributeSet
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getTotalPoints
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.pointTutorialSeen
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import kotlinx.android.synthetic.main.atom_widget_points_tutorial.view.pointsAnimation
import kotlinx.android.synthetic.main.atom_widget_points_tutorial.view.pointsTutoView

class PointsTutorialView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {
    init {
        inflate(context, R.layout.atom_widget_points_tutorial, this)
        pointTutorialSeen()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pointsAnimation.playAnimation()
        pointsTutoView.startAnimation(getTotalPoints())
    }
}
