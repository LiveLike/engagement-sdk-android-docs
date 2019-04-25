package com.livelike.livelikesdk.animation

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.airbnb.lottie.LottieAnimationView
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.easing.AnimationEaseAdapter
import com.livelike.livelikesdk.animation.easing.AnimationEaseInterpolator
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.util.AndroidResource.Companion.dpToPx
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.view.util.SwipeDismissTouchListener

internal class ViewAnimationManager(val view: View) {

    private val animationHandler = AnimationHandler()
    private val widgetShowingDurationAfterConfirmMessage: Long = 3000

    fun startWidgetTransitionInAnimation(onAnimationCompletedCallback: () -> Unit) {
        val heightToReach = view.measuredHeight.toFloat()
        // TODO: remove hardcoded start position -400 to something meaningful.
        val animator = ObjectAnimator.ofFloat(view,
            "translationY",
            -400f,
            heightToReach,
            heightToReach / 2, 0f)
        startEasingAnimation(animationHandler, AnimationEaseInterpolator.Ease.EaseOutElastic, animator)
        animationHandler.bindListenerToAnimationView(animator) { onAnimationCompletedCallback.invoke() }
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

    fun triggerTransitionOutAnimation(onCompleteCallback: (() -> Unit)?) {
        val animator = ObjectAnimator.ofFloat(
            view,
            "translationY",
            0f, -dpToPx(250).toFloat()
        )
        animationHandler.bindListenerToAnimationView(animator) {
            onCompleteCallback?.invoke()
        }
        // TODO: Get rid of hardcoded value once we have minimun viewable area defined.
        startEasingAnimation(animationHandler, AnimationEaseInterpolator.Ease.EaseOutQuad, animator)
    }

    fun startTimerAnimation(pieTimer: View, duration: Long, timerProperties: WidgetTransientState, onAnimationCompletedCallback: () -> Unit, progressUpdater: (Float) -> Unit) {
        animationHandler.startAnimation(
            pieTimer.findViewById(R.id.prediction_pie_updater_animation),
            onAnimationCompletedCallback,
            duration,
            ValueAnimator.ofFloat(timerProperties.timerAnimatorStartPhase, 1f),
            progressUpdater
        )
    }

    // TODO: Context can be injected at class level
    fun startResultAnimation(lottieAnimationPath: String,
                             context: Context,
                             prediction_result: LottieAnimationView,
                             progressUpdater: (Float) -> Unit,
                             animationPath: (String) -> Unit,
                             resultProperties: WidgetTransientState) {
        var resultAnimationPath = resultProperties.resultAnimationPath
        val resultAnimator = ValueAnimator.ofFloat(resultProperties.resultAnimatorStartPhase, 1f)

        if (resultAnimationPath == null) {
            val relativePath = AndroidResource.selectRandomLottieAnimation(lottieAnimationPath, context)
            if (relativePath != null) {
                resultAnimationPath = "$lottieAnimationPath/$relativePath"
                prediction_result.setAnimation(resultAnimationPath)
            }
        } else prediction_result.setAnimation(resultAnimationPath)

        resultAnimationPath?.let { animationPath.invoke(it) }
        prediction_result.visibility = View.VISIBLE
        animationHandler.startAnimation(prediction_result, {}, widgetShowingDurationAfterConfirmMessage, resultAnimator, {
            progressUpdater.invoke(it)
        })
    }

    fun showConfirmMessage(
        confirmMessageTextView: View,
        confirmMessageLottieAnimationView: LottieAnimationView,
        onCompleteCallback: (() -> Unit)?,
        progressUpdater: (Float) -> Unit,
        animationPath: (String) -> Unit,
        resultProperties: WidgetTransientState
    ) {

        confirmMessageTextView.visibility = View.VISIBLE

        var resultAnimationPath = resultProperties.resultAnimationPath
        val resultAnimator = ValueAnimator.ofFloat(resultProperties.resultAnimatorStartPhase, 1f)

        if (resultAnimationPath == null) {
            val lottieAnimationPath = "confirmMessage"
            val lottieAnimation = AndroidResource.selectRandomLottieAnimation(lottieAnimationPath, view.context)
            if (lottieAnimation != null) {
                resultAnimationPath = "$lottieAnimationPath/$lottieAnimation"
                confirmMessageLottieAnimationView.setAnimation(resultAnimationPath)
                confirmMessageLottieAnimationView.visibility = View.VISIBLE
            }
        } else confirmMessageLottieAnimationView.setAnimation(resultAnimationPath)

        resultAnimationPath?.let { animationPath.invoke(it) }
        confirmMessageLottieAnimationView.visibility = View.VISIBLE

        animationHandler.startAnimation(
            confirmMessageLottieAnimationView,
            { triggerTransitionOutAnimation(onCompleteCallback) },
            widgetShowingDurationAfterConfirmMessage,
            resultAnimator,
            { progressUpdater.invoke(it) })
    }

    @SuppressLint("ClickableViewAccessibility")
    fun addHorizontalSwipeListener(
        view: View,
        layout: ViewGroup,
        onSwipeCallback: (() -> Unit)?
    ) {
        view.setOnTouchListener(object : SwipeDismissTouchListener(layout,
            null, object : DismissCallbacks {
                override fun canDismiss(token: Any?) = true
                override fun onDismiss(view: View?, token: Any?) {
                    //animationHandler.cancelAnimation(timerAnimator)
                    // TODO: remove this and add as param
                    //animationHandler.cancelAnimation(resultAnimator)
                    layout.removeAllViewsInLayout()
                    onSwipeCallback?.invoke()
                }
            }
        ) {})
    }
}

private class AnimationHandler {
    fun startAnimation(lottieAnimationView: LottieAnimationView,
                       onAnimationCompletedCallback: () -> Unit,
                       duration: Long,
                       animator: ValueAnimator,
                       progressUpdater: (Float) -> Unit
    ) {
        bindListenerToAnimationView(animator, onAnimationCompletedCallback)
        animator.duration = duration
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            lottieAnimationView.progress = progress
            progressUpdater.invoke(progress)
        }

        lottieAnimationView.playAnimation()
        animator.start()
    }

    fun cancelAnimation(animator: ValueAnimator?) {
        animator?.cancel()
    }

    fun createAnimationEffectWith(ease: AnimationEaseInterpolator.Ease,
                                  forDuration: Float,
                                  animator: ValueAnimator) {
        val animatorSet = AnimatorSet()
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

    fun bindListenerToAnimationView(animator: Animator,
                                    onAnimationCompletedCallback: () -> Unit) {
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                logDebug { "Animation start" }
            }
            override fun onAnimationEnd(animation: Animator) {
                onAnimationCompletedCallback.invoke()
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

internal class AnimationProperties(val animatorStartValue: Float = 0f,
                                   val animatorEndValue: Float = 1f,
                                   val timeout: Long = 0L,
                                   val resultAnimationPath: String? = null)