package com.livelike.engagementsdk.widget.view.components

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.databinding.AtomWidgetEggTimerAndCloseButtonBinding

class EggTimerCloseButtonView(context: Context, attr: AttributeSet? = null) :
    ConstraintLayout(context, attr) {

    private var dismiss: ((action: DismissAction) -> Unit)? = null
    private var binding:AtomWidgetEggTimerAndCloseButtonBinding? = null

    init {
       // View.inflate(context, R.layout.atom_widget_egg_timer_and_close_button, this)
        binding = AtomWidgetEggTimerAndCloseButtonBinding.inflate(LayoutInflater.from(context), this@EggTimerCloseButtonView, true)
        binding?.closeButton?.setOnClickListener {
            dismiss?.invoke(DismissAction.TAP_X)
        }
    }

    fun startAnimationFrom(
        progress: Float,
        duration: Float,
        onUpdate: (Float) -> Unit,
        dismissAction: (action: DismissAction) -> Unit,
        showDismissButton: Boolean = true
    ) {
        showEggTimer()
        binding?.apply {
            eggTimer.speed = ANIMATION_BASE_TIME / duration
            eggTimer.playAnimation()
            eggTimer.pauseAnimation()
            eggTimer.progress = progress
            eggTimer.resumeAnimation()
        }

        // commenting this part, as this is not working properly since timer is getting reset (we haven't called playAnimation(),
        // instead we are calling pauseAnimation)

        // eggTimer.resumeAnimation()

        showEggTimer()
        binding?.eggTimer?.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
                //not required
            }

            override fun onAnimationCancel(animation: Animator?) {
                //not required
            }

            override fun onAnimationStart(animation: Animator?) {
                //not required
            }

            override fun onAnimationEnd(animation: Animator?) {
                binding?.eggTimer?.removeAllUpdateListeners()
                binding?.eggTimer?.removeAllAnimatorListeners()
                if (showDismissButton) {
                    showCloseButton(dismissAction)
                }
            }
        })
        binding?.eggTimer?.addAnimatorUpdateListener {
            if (it.animatedFraction < 1) {
                showEggTimer()
                onUpdate(it.animatedFraction)
            }
        }
    }

    fun showCloseButton(dismissAction: (action: DismissAction) -> Unit) {
        dismiss = dismissAction
        binding?.closeButton?.visibility = View.VISIBLE
        binding?.eggTimer?.visibility = View.GONE
    }

    private fun showEggTimer() {
        binding?.eggTimer?.visibility = View.VISIBLE
        binding?.closeButton?.visibility = View.GONE
    }

    companion object {
        private const val ANIMATION_BASE_TIME =
            5000f // This value need to be updated if the animation is changed to a different one
    }
}
