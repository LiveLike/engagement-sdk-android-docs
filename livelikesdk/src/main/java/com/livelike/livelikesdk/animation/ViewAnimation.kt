package com.livelike.livelikesdk.animation

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.View
import com.livelike.livelikesdk.R
import android.view.ViewGroup
import com.airbnb.lottie.LottieAnimationView
import com.livelike.livelikesdk.animation.easing.AnimationEaseInterpolator
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.util.AndroidResource.Companion.dpToPx
import com.livelike.livelikesdk.widget.SwipeDismissTouchListener

internal class ViewAnimation(val view: View, private val animationHandler: AnimationHandler) {
    private val widgetShowingDurationAfterConfirmMessage: Long = 3000
    private val animator = ValueAnimator.ofFloat(0f, 1f)
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

    fun triggerTransitionOutAnimation(dismissWidget: (() -> Unit)?) {
        val animator = ObjectAnimator.ofFloat(
            view,
            "translationY",
            0f, -dpToPx(250).toFloat()
        )
        animationHandler.bindListenerToAnimationView(animator) {
            dismissWidget?.invoke()
        }
        // TODO: Get rid of hardcoded value once we have minimun viewable area defined.
        startEasingAnimation(animationHandler, AnimationEaseInterpolator.Ease.EaseOutQuad, animator)
    }

    fun startTimerAnimation(pieTimer: View, duration: Long, onAnimationCompletedCallback: (Boolean) -> Unit) {
        animationHandler.startAnimation(
            pieTimer.findViewById(R.id.prediction_pie_updater_animation),
            onAnimationCompletedCallback,
            duration,
            ValueAnimator.ofFloat(0f, 1f)
        )
    }

    fun showConfirmMessage(
        confirmMessageTextView: View,
        confirmMessageLottieAnimationView: LottieAnimationView,
        dismissWidget: (() -> Unit)?
    ) {
        confirmMessageTextView.visibility = View.VISIBLE
        val lottieAnimationPath = "confirmMessage"
        val lottieAnimation = AndroidResource.selectRandomLottieAnimation(lottieAnimationPath, view.context)
        if (lottieAnimation != null) {
            confirmMessageLottieAnimationView.setAnimation("$lottieAnimationPath/$lottieAnimation")
            confirmMessageLottieAnimationView.visibility = View.VISIBLE
            animationHandler.startAnimation(
                confirmMessageLottieAnimationView,
                { triggerTransitionOutAnimation(dismissWidget) },
                widgetShowingDurationAfterConfirmMessage,
                animator
            )
        }
    }

    fun hideWidget() { view.visibility = View.INVISIBLE }

    @SuppressLint("ClickableViewAccessibility")
    fun addHorizontalSwipeListener(
        view: View,
        layout: ViewGroup,
        dismissWidget: (() -> Unit)?
    ) {
        view.setOnTouchListener(object : SwipeDismissTouchListener(layout,
            null, object : DismissCallbacks {
                override fun canDismiss(token: Any?) = true
                override fun onDismiss(view: View?, token: Any?) {
                    animationHandler.cancelAnimation(animator)
                    layout.removeAllViewsInLayout()
                    dismissWidget?.invoke()
                }
            }
        ) {})
    }
}