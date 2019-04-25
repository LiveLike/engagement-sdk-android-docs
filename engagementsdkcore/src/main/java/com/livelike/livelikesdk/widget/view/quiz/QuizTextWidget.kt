package com.livelike.livelikesdk.widget.view.quiz

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.prediction.text.TextOptionWidgetBase

internal class QuizTextWidget : TextOptionWidgetBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var viewAnimation: ViewAnimationManager
    private var fetchResult: (() -> Unit)? = null

    fun initialize(
        dismiss: () -> Unit,
        startingState: WidgetTransientState,
        progressedState: WidgetTransientState,
        fetch: () -> Unit,
        parentWidth: Int,
        viewAnimation: ViewAnimationManager,
        state: (WidgetTransientState) -> Unit
    ) {
        super.initialize(dismiss, startingState, progressedState, parentWidth, viewAnimation, state)
        fetchResult = fetch
        this.viewAnimation = viewAnimation
        startWidgetAnimation(startingState)
    }

    private fun startWidgetAnimation(properties: WidgetTransientState) {
        when {
            isWidgetDisplayedFirstTime(properties) -> viewAnimation.startWidgetTransitionInAnimation {
                pieTimerViewStub.layoutResource = R.layout.pie_timer
                val pieTimer = pieTimerViewStub.inflate()
                startPieTimer(pieTimer, properties)
                Handler().postDelayed({ dismissWidget?.invoke() }, interactionPhaseTimeout + resultPhaseTimeout)
            }
            isWidgetRestoredFromQuestionPhase(properties) -> {
                pieTimerViewStub.layoutResource = R.layout.pie_timer
                val pieTimer = pieTimerViewStub.inflate()
                startPieTimer(pieTimer, properties)
                Handler().postDelayed({ dismissWidget?.invoke() }, interactionPhaseTimeout + resultPhaseTimeout)
            }
            else -> {
                pieTimerViewStub.layoutResource = R.layout.cross_image
                pieTimerViewStub.inflate()
                showResults = true
                Handler().postDelayed({ dismissWidget?.invoke() }, resultPhaseTimeout)
            }
        }
    }

    private fun isWidgetRestoredFromQuestionPhase(properties: WidgetTransientState) =
        properties.timerAnimatorStartPhase > 0f && properties.resultAnimatorStartPhase == 0f

    private fun isWidgetDisplayedFirstTime(properties: WidgetTransientState) =
        properties.timerAnimatorStartPhase == 0f

    private fun startPieTimer(pieTimer: View, properties: WidgetTransientState) {
        viewAnimation.startTimerAnimation(pieTimer, properties.interactionPhaseTimeout, properties, {
            fetchResult?.invoke()
            showResults = true
            buttonClickEnabled = false
        }, {
            progressedState.timerAnimatorStartPhase = it
            progressedStateCallback.invoke(progressedState)
        })
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>
    ) {
        super.optionListUpdated(voteOptions, optionSelectedCallback, correctOptionWithUserSelection)
        if (showResults) {
            super.showResultsAnimation(correctOptionWithUserSelection)
        }
    }
}