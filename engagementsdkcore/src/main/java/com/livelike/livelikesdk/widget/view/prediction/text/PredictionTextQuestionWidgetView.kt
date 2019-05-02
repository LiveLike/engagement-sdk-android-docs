package com.livelike.livelikesdk.widget.view.prediction.text

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimationManager
import kotlinx.android.synthetic.main.confirm_message.view.confirmMessageTextView
import kotlinx.android.synthetic.main.confirm_message.view.prediction_confirm_message_animation
import kotlinx.android.synthetic.main.pie_timer.view.prediction_pie_updater_animation
import kotlinx.android.synthetic.main.prediction_text_widget.view.questionTextView

internal class PredictionTextQuestionWidgetView : TextOptionWidgetBase {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var viewAnimation: ViewAnimationManager

    override fun initialize(
        dismiss: () -> Unit,
        startingState: WidgetTransientState,
        progressedState: WidgetTransientState,
        parentWidth: Int,
        viewAnimation: ViewAnimationManager,
        progressedStateCallback: (WidgetTransientState) -> Unit
    ) {
        super.initialize(dismiss, startingState, progressedState, parentWidth, viewAnimation, progressedStateCallback)
        this.viewAnimation = viewAnimation
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        startWidgetAnimation(pieTimer)
    }

    private fun startWidgetAnimation(pieTimer: View) {
        if (currentPhase == WidgetTransientState.Phase.INTERACTION) {
            progressedState.currentPhase = currentPhase
            progressedStateCallback.invoke(progressedState)
            startingState.phaseTimeouts[WidgetTransientState.Phase.INTERACTION]?.let {
                startPieTimer(pieTimer, it)
                startingState.phaseTimeouts[WidgetTransientState.Phase.CONFIRM_MESSAGE]?.let { it2 ->
                    Handler().postDelayed({
                        future.cancel(false)
                        dismissWidget?.invoke() }, it + it2)
                }
            }
        } else {
            showConfirmMessage()
            performPredictionWidgetFadeOutOperations()
            startingState.phaseTimeouts[WidgetTransientState.Phase.CONFIRM_MESSAGE]?.let {
                Handler().postDelayed({
                    future.cancel(false)
                    dismissWidget?.invoke() }, it)
            }
        }
    }

    private fun startPieTimer(pieTimer: View, timeout: Long) {
        viewAnimation.startTimerAnimation(pieTimer, timeout, startingState, {
            if (optionSelectedId.isNotEmpty()) {
                showConfirmMessage()
                performPredictionWidgetFadeOutOperations()
            } else {
                Handler().postDelayed({
                    future.cancel(false)
                    dismissWidget?.invoke() }, timeout)
            }
        }, {
            progressedState.timerAnimatorStartPhase = it
            progressedStateCallback.invoke(progressedState)
        })
    }

    private fun showConfirmMessage() {
        currentPhase = WidgetTransientState.Phase.CONFIRM_MESSAGE
        progressedState.currentPhase = currentPhase
        progressedStateCallback.invoke(progressedState)
        viewAnimation.showConfirmMessage(
            confirmMessageTextView,
            prediction_confirm_message_animation,
            {},
            {
                progressedState.resultAnimatorStartPhase = it
                progressedStateCallback.invoke(progressedState)
            },
            {
                progressedState.resultAnimationPath = it
                progressedStateCallback.invoke(progressedState)
            },
            startingState
        )
    }

    private fun performPredictionWidgetFadeOutOperations() {
        buttonList.forEach { button ->
            disableButtons(button)
            button.setTranslucent()
        }
        questionTextView.setTranslucent()
        prediction_pie_updater_animation.setTranslucent()
    }

    private fun View.setTranslucent() {
        this.alpha = widgetOpacityFactor
    }

    private fun disableButtons(button: Button) {
        button.isEnabled = false
    }
}