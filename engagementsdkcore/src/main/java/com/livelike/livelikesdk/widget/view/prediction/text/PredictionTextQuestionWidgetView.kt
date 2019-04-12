package com.livelike.livelikesdk.widget.view.prediction.text

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.animation.AnimationProperties
import com.livelike.livelikesdk.animation.ViewAnimationManager
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.pie_timer.view.*
import kotlinx.android.synthetic.main.prediction_text_widget.view.*

internal class PredictionTextQuestionWidgetView : TextOptionWidgetBase {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    private lateinit var viewAnimation: ViewAnimationManager

    override fun initialize(dismiss: ()->Unit,
                            startingState: WidgetTransientState,
                            progressedState: WidgetTransientState,
                            parentWidth: Int,
                            viewAnimation: ViewAnimationManager,
                            progressedStateCallback: (WidgetTransientState) -> Unit) {
        super.initialize(dismiss, startingState, progressedState, parentWidth, viewAnimation, progressedStateCallback)
        this.viewAnimation = viewAnimation
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        startWidgetAnimation(pieTimer, startingState.timeout)
    }

    private fun startWidgetAnimation(pieTimer: View, timeout : Long) {
        if (startingState.timerAnimatorStartPhase != 0f) {
            startPieTimer(pieTimer, timeout)
        }
        else viewAnimation.startWidgetTransitionInAnimation {
            startPieTimer(pieTimer, timeout)
        }
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout)
    }

    private fun startPieTimer(pieTimer: View, timeout: Long) {
        viewAnimation.startTimerAnimation(pieTimer, timeout, startingState, {
            if (optionSelectedId.isNotEmpty()) {
                viewAnimation.showConfirmMessage(
                    prediction_confirm_message_textView,
                    prediction_confirm_message_animation
                ) {}
                performPredictionWidgetFadeOutOperations()
            }
        }, {
            progressedState.timerAnimatorStartPhase = it
            progressedStateCallback.invoke(progressedState)
        })
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