package com.livelike.livelikesdk.widget.view.quiz

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.prediction.text.TextOptionWidgetBase

internal class QuizTextWidget : TextOptionWidgetBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    private lateinit var viewAnimation: ViewAnimation
    private var fetchResult: (() -> Unit)? = null

    fun initialize(dismiss : ()->Unit, timeout : Long, fetch: () -> Unit, parentWidth: Int, viewAnimation: ViewAnimation, state: (WidgetTransientState) -> Unit) {
        super.initialize(dismiss, timeout, parentWidth, viewAnimation, state)
        fetchResult = fetch
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        startWidgetAnimation(pieTimer, timeout)
    }

    private fun startWidgetAnimation(pieTimer: View, timeout: Long) {
        viewAnimation.startWidgetTransitionInAnimation {
            viewAnimation.startTimerAnimation(pieTimer, timeout, {
                showResults = true
                buttonClickEnabled = false
                fetchResult?.invoke()
            }, {})
        }
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout * 2)
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>
    ) {
        super.optionListUpdated(voteOptions, optionSelectedCallback, correctOptionWithUserSelection)
        if(showResults) {
            super.showResultsAnimation(correctOptionWithUserSelection)
        }
    }
}