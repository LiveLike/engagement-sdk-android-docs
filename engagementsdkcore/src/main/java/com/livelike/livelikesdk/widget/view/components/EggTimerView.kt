package com.livelike.livelikesdk.widget.view.components

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.atom_widget_egg_timer.view.eggTimer

class EggTimerView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {
    val ANIMATION_BASE_TIME = 5000f // This value need to be updated if the animation is changed to a different one

    init {
        View.inflate(context, R.layout.atom_widget_egg_timer, this)
    }

    fun startAnimationFrom(progress: Float, duration: Float, onUpdate: (Float) -> Unit) {
        eggTimer.speed = ANIMATION_BASE_TIME / duration
        eggTimer.progress = progress
        eggTimer.resumeAnimation()
        eggTimer.addAnimatorUpdateListener {
            if (it.animatedFraction < 1) {
                onUpdate(it.animatedFraction)
            } else {
                eggTimer.removeAllUpdateListeners()
            }
        }
    }
}