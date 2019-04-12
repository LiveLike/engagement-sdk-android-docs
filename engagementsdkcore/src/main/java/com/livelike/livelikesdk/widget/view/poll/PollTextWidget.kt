package com.livelike.livelikesdk.widget.view.poll

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.widget.view.prediction.text.TextOptionWidgetBase

internal class PollTextWidget : TextOptionWidgetBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    private lateinit var viewAnimation: ViewAnimationManager
    private var fetchResult: (() -> Unit)? = null

    fun initialize(dismiss : ()->Unit,
                   startingState: WidgetTransientState,
                   progressedState: WidgetTransientState,
                   fetch: () -> Unit,
                   parentWidth: Int,
                   viewAnimation: ViewAnimationManager,
                   state: (WidgetTransientState) -> Unit) {
        super.initialize(dismiss, startingState, progressedState, parentWidth, viewAnimation, state)
        fetchResult = fetch
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        this.viewAnimation = viewAnimation
        startWidgetAnimation(pieTimer, startingState)
        showResults = true
    }

    private fun startWidgetAnimation(pieTimer: View, properties: WidgetTransientState) {
        viewAnimation.startWidgetTransitionInAnimation {
            viewAnimation.startTimerAnimation(pieTimer, properties.timeout, properties, {
                showResults = true
                fetchResult?.invoke()
            }, {
                progressedState.timerAnimatorStartPhase = it
                progressedStateCallback.invoke(progressedState)
            })
        }
        Handler().postDelayed({ dismissWidget?.invoke() }, properties.timeout * 2)
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        if(prevOptionSelectedId != optionSelectedId)
            fetchResult?.invoke()
    }
}