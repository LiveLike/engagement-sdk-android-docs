package com.livelike.livelikesdk.animation

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import com.airbnb.lottie.LottieAnimationView
import com.livelike.livelikesdk.animation.easing.AnimationEaseAdapter
import com.livelike.livelikesdk.animation.easing.AnimationEaseInterpolator
import com.livelike.livelikesdk.util.logDebug

class AnimationHandler {
    private var animator: ValueAnimator = ValueAnimator.ofFloat(0f, 0f)
    fun startAnimation(lottieAnimationView: LottieAnimationView,
                       onAnimationCompletedCallback: (Boolean) -> Unit,
                       duration: Long) {
        bindListenerToAnimationView(animator, onAnimationCompletedCallback)
        animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration
        animator.addUpdateListener { animation ->
            lottieAnimationView.progress = animation.animatedValue as Float
        }

        lottieAnimationView.playAnimation()
        animator.start()
    }

    fun cancelAnimation() {
        animator.cancel()
    }

    fun createAnimationEffectWith(ease: AnimationEaseInterpolator.Ease,
                                  forDuration: Float,
                                  animator: ValueAnimator) {
        val animatorSet = AnimatorSet()

        // TODO: remove hardcoded start position -400 to something meaningful.
        animatorSet.playTogether(
            AnimationEaseAdapter()
                .createAnimationEffectWith(
                        ease,
                        forDuration,
                        animator
                ))

        animatorSet.duration = forDuration.toLong()
        animatorSet.start()
    }

    fun bindListenerToAnimationView(view: Animator,
                                    onAnimationCompletedCallback: (Boolean) -> Unit) {
        view.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                logDebug { "Animation start" }
            }
            override fun onAnimationEnd(animation: Animator) {
                onAnimationCompletedCallback(true)
            }

            override fun onAnimationCancel(animation: Animator) {
                logDebug { "Animation cancel" }
            }

            override fun onAnimationRepeat(animation: Animator) {
                logDebug { "Animation repeat" }
            }
        })
    }
}