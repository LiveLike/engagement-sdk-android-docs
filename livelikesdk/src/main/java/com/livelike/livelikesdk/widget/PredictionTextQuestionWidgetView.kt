package com.livelike.livelikesdk.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import com.livelike.livelikesdk.animation.AnimationHandler
import kotlinx.android.synthetic.main.pie_timer.view.*
import kotlinx.android.synthetic.main.prediction_text_widget.view.*
import com.livelike.livelikesdk.R

class PredictionTextQuestionWidgetView  : PredictionTextWidgetBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        startWidgetAnimation(pieTimer)
    }

    private fun startWidgetAnimation(pieTimer: View) {
        startEasingAnimation(animationHandler)
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
            prediction_confirm_message_animation.setAnimation(lottieAnimationPath + '/' + selectRandomEmojiForConfirmMessage(lottieAnimationPath))
            prediction_confirm_message_animation.visibility = View.VISIBLE
            animationHandler.startAnimation(
                    prediction_confirm_message_animation,
                    { hideWidget() },
                    widgetShowingDurationAfterConfirmMessage)
            performPredictionWidgetFadeOutOperations()
        } else hideWidget()
    }

    private fun hideWidget() {
        prediction_text_widget.visibility = View.INVISIBLE
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