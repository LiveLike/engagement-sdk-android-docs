package com.livelike.livelikesdk.widget.view.prediction.text

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.pie_timer.view.*
import kotlinx.android.synthetic.main.prediction_text_widget.view.*

internal class PredictionTextQuestionWidgetView : PredictionTextWidgetBase {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int,  dismiss: () -> Unit) : super(context, attrs, defStyleAttr, dismiss)
    private var viewAnimation: ViewAnimation

    init {
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        viewAnimation = ViewAnimation(this)
        startWidgetAnimation(pieTimer)
    }

    private fun startWidgetAnimation(pieTimer: View) {
        viewAnimation.startWidgetTransitionInAnimation {
            viewAnimation.startTimerAnimation(pieTimer, 7000) {
                if (optionSelected) {
                    viewAnimation.showConfirmMessage(
                        prediction_confirm_message_textView,
                        prediction_confirm_message_animation,
                        dismissWidget
                    )
                    performPredictionWidgetFadeOutOperations()
                } else {
                    viewAnimation.hideWidget()
                    dismissWidget?.invoke()
                }
            }
        }
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