package com.livelike.livelikesdk.widget.view

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.AnimationHandler
import com.livelike.livelikesdk.animation.easing.AnimationEaseInterpolator
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.pie_timer.view.*
import kotlinx.android.synthetic.main.prediction_text_widget.view.*

class PredictionTextQuestionWidgetView  : PredictionTextWidgetBase {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int,  dismiss: () -> Unit) : super(context, attrs, defStyleAttr, dismiss)

    init {
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        startWidgetAnimation(pieTimer)
    }

    private fun startWidgetAnimation(pieTimer: View) {
        val heightToReach = this.measuredHeight.toFloat()
        // TODO: remove hardcoded start position -400 to something meaningful.
        val animator = ObjectAnimator.ofFloat(this,
            "translationY",
            -400f,
            heightToReach,
            heightToReach / 2, 0f)
        startEasingAnimation(animationHandler, AnimationEaseInterpolator.Ease.EaseOutElastic, animator)
        startTimerAnimation(pieTimer)
    }

    private fun startTimerAnimation(pieTimer: View) {
        animationHandler.startAnimation(
                pieTimer.findViewById(R.id.prediction_pie_updater_animation),
                { onTimerAnimationCompleted(animationHandler) },
                timerDuration)
    }

    private fun onTimerAnimationCompleted(animationHandler: AnimationHandler) {
        if (optionSelected) {
            prediction_confirm_message_textView.visibility = View.VISIBLE
            lottieAnimationPath = "confirmMessage"
            val lottieAnimation = selectRandomLottieAnimation(lottieAnimationPath)
            if (lottieAnimation != null && prediction_confirm_message_animation != null) {
                prediction_confirm_message_animation.setAnimation("$lottieAnimationPath/$lottieAnimation")
                prediction_confirm_message_animation.visibility = View.VISIBLE
                animationHandler.startAnimation(
                    prediction_confirm_message_animation,
                    { dismissWidget() },
                    widgetShowingDurationAfterConfirmMessage
                )
                performPredictionWidgetFadeOutOperations()
            }
        } else dismissWidget()
    }

    private fun performPredictionWidgetFadeOutOperations() {
        buttonList.forEach { button ->
            disableButtons(button)
            button.setTranslucent()
        }
        prediction_question_textView.setTranslucent()
        prediction_pie_updater_animation.setTranslucent()
    }

    private fun View.setTranslucent() {
        this.alpha = widgetOpacityFactor
    }

    private fun disableButtons(button: Button) {
        button.isEnabled = false
    }
}