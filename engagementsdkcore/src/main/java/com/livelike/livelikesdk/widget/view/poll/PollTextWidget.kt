package com.livelike.livelikesdk.widget.view.poll

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.prediction.text.TextOptionWidgetBase

internal class PollTextWidget : TextOptionWidgetBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    private lateinit var viewAnimation: ViewAnimation
    private var fetchResult: (() -> Unit)? = null


    fun initialize(dismiss: () -> Unit, timeout: Long, fetch: () -> Unit, parentWidth: Int) {
        super.initialize(dismiss, timeout, parentWidth)
        fetchResult = fetch
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        viewAnimation = ViewAnimation(this)
        startWidgetAnimation(pieTimer, timeout)
        showResults = true
    }

    private fun startWidgetAnimation(pieTimer: View, timeout: Long) {
        viewAnimation.startWidgetTransitionInAnimation {
            viewAnimation.startTimerAnimation(pieTimer, timeout) {}
        }
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout * 2)
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        if(prevOptionSelectedId != optionSelectedId)
            fetchResult?.invoke()
    }
}