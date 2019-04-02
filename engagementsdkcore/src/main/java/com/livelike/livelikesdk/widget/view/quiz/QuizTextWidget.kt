package com.livelike.livelikesdk.widget.view.quiz

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.binding.QuizVoteObserver
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.prediction.text.TextOptionWidgetBase
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.pie_timer.view.*
import kotlinx.android.synthetic.main.prediction_text_widget.view.*

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
                showResults = true
                fetchResult?.invoke()
                Handler().postDelayed({ viewAnimation.triggerTransitionOutAnimation { dismissWidget?.invoke() } }, timeout)
            }
        }
    }
}