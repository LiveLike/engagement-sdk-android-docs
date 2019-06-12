package com.livelike.livelikesdk.widget.view.components

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.atom_widget_egg_timer_and_close_button.view.closeButton
import kotlinx.android.synthetic.main.atom_widget_egg_timer_and_close_button.view.eggTimer

class EggTimerCloseButtonView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {
    private val ANIMATION_BASE_TIME =
        5000f // This value need to be updated if the animation is changed to a different one

    private var session: LiveLikeContentSession? = null

    init {
        View.inflate(context, R.layout.atom_widget_egg_timer_and_close_button, this)
        closeButton.setOnClickListener {
            session?.currentWidgetInfosStream?.onNext(null)
        }
    }

    fun startAnimationFrom(
        progress: Float,
        duration: Float,
        onUpdate: (Float) -> Unit,
        session: LiveLikeContentSession?
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
                showCloseButton(session)
            }
        }
    }

    fun showCloseButton(session: LiveLikeContentSession?) {
        this.session = session
        closeButton.visibility = View.VISIBLE
        eggTimer.visibility = View.GONE
    }

    fun showEggTimer() {
        eggTimer.visibility = View.VISIBLE
        closeButton.visibility = View.GONE
    }
}