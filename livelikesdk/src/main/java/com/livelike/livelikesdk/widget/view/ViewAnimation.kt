package com.livelike.livelikesdk.widget.view

import android.animation.ObjectAnimator
import android.content.res.Resources
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.livelike.livelikesdk.animation.AnimationEaseInterpolator
import com.livelike.livelikesdk.animation.AnimationHandler
import java.util.*
import com.livelike.livelikesdk.R

class ViewAnimation(val view: View, private val animationHandler: AnimationHandler) {
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
            val lottieAnimation = selectRandomLottieAnimation(lottieAnimationPath)
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

    private fun hideWidget() {
        view.visibility = View.INVISIBLE
    }

    // TODO: move these to util classes.
    protected fun dpToPx(dp: Int): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    protected fun selectRandomLottieAnimation(path: String): String? {
        val asset = view.context?.assets
        val assetList = asset?.list(path)
        val random = Random()
        return if (assetList!!.isNotEmpty()) {
            val emojiIndex = random.nextInt(assetList.size)
            assetList[emojiIndex]
        } else return null
    }
}