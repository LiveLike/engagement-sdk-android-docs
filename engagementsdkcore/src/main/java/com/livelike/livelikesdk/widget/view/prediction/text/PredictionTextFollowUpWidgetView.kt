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
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        showResults = true
    }

    companion object {
        const val correctAnswerLottieFilePath = "correctAnswer"
        const val wrongAnswerLottieFilePath = "wrongAnswer"
    }
    private lateinit var viewAnimation: ViewAnimationManager
    private var timeout = 0L
    private var initialTimeout = 0L
    private var executor = ScheduledThreadPoolExecutor(15)
    lateinit var future: ScheduledFuture<*>

    override fun initialize(dismiss: ()->Unit, timeout: Long, parentWidth: Int, viewAnimation: ViewAnimationManager, state: (WidgetTransientState) -> Unit) {
        super.initialize(dismiss, timeout, parentWidth, viewAnimation, state)
        showResults = true
        buttonClickEnabled = false
        this.viewAnimation = viewAnimation
        pieTimerViewStub.layoutResource = R.layout.cross_image
        pieTimerViewStub.inflate()
        val imageView = findViewById<ImageView>(R.id.prediction_followup_image_cross)
        imageView.setImageResource(R.mipmap.widget_ic_x)
        imageView.setOnClickListener { dismissWidget() }
        this.timeout = timeout
        future = executor.scheduleAtFixedRate(Updater(), 0, 1, TimeUnit.SECONDS)
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>) {
        super.optionListUpdated(voteOptions, optionSelectedCallback, correctOptionWithUserSelection)
        super.showResultsAnimation(correctOptionWithUserSelection)
        transitionAnimation()
    }

    private fun transitionAnimation() {
        viewAnimation.startWidgetTransitionInAnimation{
//            viewAnimation.startResultAnimation(lottieAnimationPath, context, prediction_result, {
//                transientState.pieTimerProgress = it
//                state.invoke(transientState)
//            }, {
//                transientState.resultPath = it
//                state.invoke(transientState)
//            })
        }
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout)
    }

    inner class Updater: Runnable {
        override fun run() {
            transientState.timeout = timeout - initialTimeout
            state.invoke(transientState)
            val updateRate = 1000
            initialTimeout += updateRate
            if (timeout == initialTimeout) {
                future.cancel(false)
            }
        }
    }
}
