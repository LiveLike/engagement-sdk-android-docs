package com.livelike.livelikesdk.animation

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.util.Log
import com.airbnb.lottie.LottieAnimationView
import com.livelike.livelikesdk.R

class AnimationHandler {
    private val TAG = this::class.java.simpleName
    fun startAnimation(lottieAnimationView: LottieAnimationView,
                       onAnimationCompletedCallback: (Boolean) -> Unit,
                       duration: Long) {
        bindListenerToTimerAnimation(lottieAnimationView, onAnimationCompletedCallback)
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration
        animator.addUpdateListener { animation ->
            lottieAnimationView.progress = animation.animatedValue as Float
        }

        lottieAnimationView.playAnimation()
        animator.start()
    }

    fun createAnimationEffectWith(ease: AnimationEaseInterpolator.Ease,
                                  forDuration: Float,
                                  animator: ValueAnimator) {
        val animatorSet = AnimatorSet()

        // TODO: remove hardcoded start position -400 to something meaningful.
        animatorSet.playTogether(AnimationEaseAdapter()
                .createAnimationEffectWith(
                        ease,
                        forDuration,
                        animator
                ))

        animatorSet.duration = forDuration.toLong()
        animatorSet.start()
    }

    private fun bindListenerToTimerAnimation(lottieAnimationView: LottieAnimationView,
                                             onAnimationCompletedCallback: (Boolean) -> Unit) {
        lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) { Log.d(TAG, "Animation start") }
            override fun onAnimationEnd(animation: Animator) {
                onAnimationCompletedCallback(true)
            }
            override fun onAnimationCancel(animation: Animator) { Log.d(TAG, "Animation cancel") }
            override fun onAnimationRepeat(animation: Animator) { Log.d(TAG, "Animation repeat") }
        })
    }
}