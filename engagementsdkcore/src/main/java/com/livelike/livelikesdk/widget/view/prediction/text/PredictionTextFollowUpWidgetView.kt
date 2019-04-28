package com.livelike.livelikesdk.widget.view.prediction.text

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.widget.ImageView
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.widget.model.VoteOption
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class PredictionTextFollowUpWidgetView :
    TextOptionWidgetBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var viewAnimation: ViewAnimationManager
    private var timeout = 0L

    override fun initialize(
        dismiss: () -> Unit,
        startingState: WidgetTransientState,
        progressedState: WidgetTransientState,
        parentWidth: Int,
        viewAnimation: ViewAnimationManager,
        progressedStateCallback: (WidgetTransientState) -> Unit
    ) {
        super.initialize(dismiss, startingState, progressedState, parentWidth, viewAnimation, progressedStateCallback)
        showResults = true
        buttonClickEnabled = false
        this.viewAnimation = viewAnimation
        pieTimerViewStub.layoutResource = R.layout.cross_image
        pieTimerViewStub.inflate()
        val imageView = findViewById<ImageView>(R.id.prediction_followup_image_cross)
        imageView.setImageResource(R.mipmap.widget_ic_x)
        imageView.setOnClickListener { dismissWidget() }
        this.timeout = startingState.interactionPhaseTimeout
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>
    ) {
        super.optionListUpdated(voteOptions, optionSelectedCallback, correctOptionWithUserSelection)
        super.showResultsAnimation(correctOptionWithUserSelection)
        transitionAnimation()
    }

    private fun transitionAnimation() {
        viewAnimation.startWidgetTransitionInAnimation {
            //            viewAnimation.startResultAnimation(lottieAnimationPath, context, prediction_result, {
//                transientState.pieTimerProgress = it
//                state.invoke(transientState)
//            }, {
//                transientState.resultPath = it
//                state.invoke(transientState)
//            })
        viewAnimation.startWidgetTransitionInAnimation{
        }
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout)
    }
    }
    inner class Updater : Runnable {
        override fun run() {
            progressedState.interactionPhaseTimeout = timeout - initialTimeout
            progressedStateCallback.invoke(progressedState)
            val updateRate = 1000
            initialTimeout += updateRate
            if (timeout == initialTimeout) {
                future.cancel(false)
            }
        }
    }

}
