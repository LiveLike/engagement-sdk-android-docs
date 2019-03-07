package com.livelike.livelikesdk.animation

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.View
import com.livelike.livelikesdk.R
import android.view.ViewGroup
import com.airbnb.lottie.LottieAnimationView
import com.livelike.livelikesdk.animation.easing.AnimationEaseInterpolator
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.widget.SwipeDismissTouchListener

internal class ViewAnimation(val view: View, private val animationHandler: AnimationHandler) {
    private val widgetShowingDurationAfterConfirmMessage: Long = 3000

    fun startWidgetTransitionInAnimation() {
        val heightToReach = view.measuredHeight.toFloat()
        // TODO: remove hardcoded start position -400 to something meaningful.
        val animator = ObjectAnimator.ofFloat(view,
            "translationY",
            -400f,
            heightToReach,
            heightToReach / 2, 0f)
        startEasingAnimation(animationHandler, AnimationEaseInterpolator.Ease.EaseOutElastic, animator)
    }

    // Would have to think more on how to not use hard coded values. I think once we have more easing
    // functions to use and how we layout widget and chat we can think of these values more.
    private fun startEasingAnimation(
        animationHandler: AnimationHandler,
        ease: AnimationEaseInterpolator.Ease,
        animator: ObjectAnimator) {
        val animationDuration = 1000f
        when(ease)  {
            AnimationEaseInterpolator.Ease.EaseOutElastic -> {
                animationHandler.createAnimationEffectWith(
                    ease,
                    animationDuration,
                    animator)
            }
            AnimationEaseInterpolator.Ease.EaseOutQuad -> {
                animationHandler.createAnimationEffectWith(
                    ease,
                    animationDuration,
                    animator
                )
            }
            else -> {}
        }
    }

    fun startTimerAnimation(pieTimer: View, duration: Long, onAnimationCompletedCallback: (Boolean) -> Unit) {
        animationHandler.startAnimation(
            pieTimer.findViewById(R.id.prediction_pie_updater_animation),
            onAnimationCompletedCallback,
            duration)
    }

    fun showConfirmMessage(confirmMessageTextView: View,
                           confirmMessageLottieAnimationView: LottieAnimationView) {
        confirmMessageTextView.visibility = View.VISIBLE
        val lottieAnimationPath = "confirmMessage"
        val lottieAnimation = AndroidResource.selectRandomLottieAnimation(lottieAnimationPath, view.context)
        if (lottieAnimation != null) {
            confirmMessageLottieAnimationView.setAnimation("$lottieAnimationPath/$lottieAnimation")
            confirmMessageLottieAnimationView.visibility = View.VISIBLE
            animationHandler.startAnimation(
                confirmMessageLottieAnimationView,
                { hideWidget() },
                widgetShowingDurationAfterConfirmMessage
            )
        }
    }

    fun hideWidget() { view.visibility = View.INVISIBLE }

    @SuppressLint("ClickableViewAccessibility")
    fun addHorizontalSwipeListener(view: View, layout: ViewGroup) {
        view.setOnTouchListener(object : SwipeDismissTouchListener(layout,
            null, object : DismissCallbacks {
                override fun canDismiss(token: Any?) = true
                override fun onDismiss(view: View?, token: Any?) {
                    layout.removeAllViewsInLayout()
                    layout.visibility = View.INVISIBLE
                }
            }
        ) {})
    }
}