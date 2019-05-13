package com.livelike.livelikesdk.widget.view.prediction.text

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.widget.Button
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.animation.ViewAnimationManager

internal class PredictionTextQuestionWidgetView : ConstraintLayout {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var viewAnimation: ViewAnimationManager

    fun initialize(
        dismiss: () -> Unit,
        startingState: WidgetTransientState,
        progressedState: WidgetTransientState,
        fetch: () -> Unit,
        parentWidth: Int,
        viewAnimation: ViewAnimationManager,
        progressedStateCallback: (WidgetTransientState) -> Unit
    ) {
        this.viewAnimation = viewAnimation
//        pieTimerViewStub.layoutResource = R.layout.pie_timer
//        val pieTimer = pieTimerViewStub.inflate()
//        startWidgetAnimation(pieTimer)
    }

//    private fun startWidgetAnimation(pieTimer: View) {
//        if (currentPhase == WidgetTransientState.Phase.INTERACTION) {
//            progressedState.currentPhase = currentPhase
//            progressedStateCallback.invoke(progressedState)
//            startingState.phaseTimeouts[WidgetTransientState.Phase.INTERACTION]?.let {
//                startPieTimer(pieTimer, it)
//                startingState.phaseTimeouts[WidgetTransientState.Phase.CONFIRM_MESSAGE]?.let { it2 ->
//                    Handler().postDelayed({
//                        future.cancel(false)
//                        dismissWidget?.invoke() }, it + it2)
//                }
//            }
//        } else {
//            showConfirmMessage()
//            performPredictionWidgetFadeOutOperations()
//            startingState.phaseTimeouts[WidgetTransientState.Phase.CONFIRM_MESSAGE]?.let {
//                Handler().postDelayed({
//                    future.cancel(false)
//                    dismissWidget?.invoke() }, it)
//            }
//        }
//    }

//    private fun startPieTimer(pieTimer: View, timeout: Long) {
//        viewAnimation.startTimerAnimation(pieTimer, timeout, startingState, {
//            if (optionSelectedId.isNotEmpty()) {
//                fetch?.invoke()
//                showConfirmMessage()
//                performPredictionWidgetFadeOutOperations()
//            } else {
//                Handler().postDelayed({
//                    future.cancel(false)
//                    dismissWidget?.invoke() }, timeout)
//            }
//        }, {
//            progressedState.timerAnimatorStartPhase = it
//            progressedStateCallback.invoke(progressedState)
//        })
//    }
//
//    private fun showConfirmMessage() {
//        currentPhase = WidgetTransientState.Phase.CONFIRM_MESSAGE
//        progressedState.currentPhase = currentPhase
//        progressedStateCallback.invoke(progressedState)
//        viewAnimation.showConfirmMessage(
//            confirmMessageTextView,
//            prediction_confirm_message_animation,
//            {},
//            {
//                progressedState.resultAnimatorStartPhase = it
//                progressedStateCallback.invoke(progressedState)
//            },
//            {
//                progressedState.resultAnimationPath = it
//                progressedStateCallback.invoke(progressedState)
//            },
//            startingState
//        )
//    }

//    private fun performPredictionWidgetFadeOutOperations() {
//        buttonList.forEach { button ->
//            disableButtons(button)
//            button.setTranslucent()
//        }
//        questionTextView.setTranslucent()
//        prediction_pie_updater_animation.setTranslucent()
//    }
//
//    private fun View.setTranslucent() {
//        this.alpha = widgetOpacityFactor
//    }

    private fun disableButtons(button: Button) {
        button.isEnabled = false
    }
}