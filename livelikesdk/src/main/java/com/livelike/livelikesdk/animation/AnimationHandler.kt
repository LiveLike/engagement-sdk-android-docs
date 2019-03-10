package com.livelike.livelikesdk.animation

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.util.Log
import com.airbnb.lottie.LottieAnimationView
import com.livelike.livelikesdk.animation.easing.AnimationEaseAdapter
import com.livelike.livelikesdk.animation.easing.AnimationEaseInterpolator

class AnimationHandler {
    private val TAG = this::class.java.simpleName
    lateinit var animator: ValueAnimator
    fun startAnimation(lottieAnimationView: LottieAnimationView,
                       onAnimationCompletedCallback: (Boolean) -> Unit,
                       duration: Long) {
        animator = ValueAnimator.ofFloat(0f, 1f)
        bindListenerToAnimationView(animator, onAnimationCompletedCallback)
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
            override fun onAnimationStart(animation: Animator) { Log.d(TAG, "Animation start") }
            override fun onAnimationEnd(animation: Animator) {
                onAnimationCompletedCallback(true)
            }
            override fun onAnimationCancel(animation: Animator) { Log.d(TAG, "Animation cancel") }
            override fun onAnimationRepeat(animation: Animator) { Log.d(TAG, "Animation repeat") }
        })
    }
}