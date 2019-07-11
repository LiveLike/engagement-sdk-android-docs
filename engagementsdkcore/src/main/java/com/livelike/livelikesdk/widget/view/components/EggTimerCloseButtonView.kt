package com.livelike.livelikesdk.widget.view.components

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.widget.DismissAction
import kotlinx.android.synthetic.main.atom_widget_egg_timer_and_close_button.view.closeButton
import kotlinx.android.synthetic.main.atom_widget_egg_timer_and_close_button.view.eggTimer

class EggTimerCloseButtonView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {
    private val ANIMATION_BASE_TIME =
        5000f // This value need to be updated if the animation is changed to a different one

    private var dismiss: ((action: DismissAction) -> Unit)? = null

    init {
        View.inflate(context, R.layout.atom_widget_egg_timer_and_close_button, this)
        closeButton.setOnClickListener {
            dismiss?.invoke(DismissAction.TAP_X)
        }
    }

    fun startAnimationFrom(
        progress: Float,
        duration: Float,
        onUpdate: (Float) -> Unit,
        dismissAction: (action: DismissAction) -> Unit
    ) {
        eggTimer.speed = ANIMATION_BASE_TIME / duration
        eggTimer.progress = progress
        eggTimer.resumeAnimation()
        eggTimer.addAnimatorUpdateListener {
            if (it.animatedFraction < 1) {
                showEggTimer()
                onUpdate(it.animatedFraction)
            } else {
                eggTimer.removeAllUpdateListeners()
                showCloseButton(dismissAction)
            }
        }
    }

    fun showCloseButton(dismissAction: (action: DismissAction) -> Unit) {
        dismiss = dismissAction
        closeButton.visibility = View.VISIBLE
        eggTimer.visibility = View.GONE
    }

    fun showEggTimer() {
        eggTimer.visibility = View.VISIBLE
        closeButton.visibility = View.GONE
    }
}