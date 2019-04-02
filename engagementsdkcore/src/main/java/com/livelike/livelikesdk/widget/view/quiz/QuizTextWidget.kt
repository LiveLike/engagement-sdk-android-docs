package com.livelike.livelikesdk.widget.view.quiz

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.widget.view.prediction.text.TextOptionWidgetBase

internal class QuizTextWidget : TextOptionWidgetBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    private lateinit var viewAnimation: ViewAnimation
    private var fetchResult: (() -> Unit)? = null

    fun initialize(dismiss : ()->Unit, timeout : Long, fetch: () -> Unit) {
        super.initialize(dismiss, timeout)
        fetchResult = fetch
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        viewAnimation = ViewAnimation(this)
        startWidgetAnimation(pieTimer, timeout)
    }

    private fun startWidgetAnimation(pieTimer: View, timeout: Long) {
        viewAnimation.startWidgetTransitionInAnimation {
            viewAnimation.startTimerAnimation(pieTimer, timeout) {
                if (!showResults) {
                    showResults = true
                    buttonList.forEach { button ->
                        disableButtons(button)
                    }
                    fetchResult?.invoke()
                }
            }
        }
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout)
    }

    private fun disableButtons(button: Button) {
        button.isEnabled = false
    }
}