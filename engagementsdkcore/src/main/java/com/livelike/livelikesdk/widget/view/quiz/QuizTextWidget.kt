package com.livelike.livelikesdk.widget.view.quiz

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.widget.view.prediction.text.TextOptionWidgetBase

internal class QuizTextWidget : TextOptionWidgetBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    private lateinit var viewAnimation: ViewAnimationManager
    private var fetchResult: (() -> Unit)? = null

    fun initialize(dismiss : ()->Unit,
                   properties: WidgetTransientState,
                   fetch: () -> Unit,
                   parentWidth: Int,
                   viewAnimation: ViewAnimationManager,
                   state: (WidgetTransientState) -> Unit) {
        super.initialize(dismiss, properties, parentWidth, viewAnimation, state)
        fetchResult = fetch
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        this.viewAnimation = viewAnimation
        val pieTimer = pieTimerViewStub.inflate()
        startWidgetAnimation(pieTimer, properties)
    }

    private fun startWidgetAnimation(pieTimer: View, properties: WidgetTransientState) {
        if (properties.timerAnimatorStartPhase != 1f) {
            viewAnimation.startWidgetTransitionInAnimation {
                viewAnimation.startTimerAnimation(pieTimer, properties.timeout, properties, {
                    showResults = true
                    buttonClickEnabled = false
                    fetchResult?.invoke()
                }, {
                    transientState.timerAnimatorStartPhase = it
                    state.invoke(transientState)
                })
            }
        }
        Handler().postDelayed(
            { dismissWidget?.invoke() }, properties.timeout * 2)
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