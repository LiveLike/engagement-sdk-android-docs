package com.livelike.livelikesdk.widget.view.prediction.image

import android.annotation.SuppressLint
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewStub
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.adapters.ImageAdapter
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil
import kotlinx.android.synthetic.main.confirm_message.view.prediction_result
import kotlinx.android.synthetic.main.cross_image.view.prediction_followup_image_cross
import kotlinx.android.synthetic.main.prediction_image_widget.view.imageOptionList
import kotlinx.android.synthetic.main.prediction_image_widget.view.questionTextView
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class PredictionImageFollowupWidget : ConstraintLayout, WidgetObserver {
    private var dismissWidget: (() -> Unit)? = null
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimationManager
    private lateinit var progressedStateCallback: (WidgetTransientState) -> Unit
    private lateinit var progressedState: WidgetTransientState
    private lateinit var resultDisplayUtil: WidgetResultDisplayUtil
    private lateinit var startingState: WidgetTransientState
    private var layout = ConstraintLayout(context, null, 0)
    private var timeout = 0L
    private var initialTimeout = 0L
    var parentWidth = 0
    private var imageAdapter : ImageAdapter? = null

    private var executor = ScheduledThreadPoolExecutor(15)
    lateinit var future: ScheduledFuture<*>

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun initialize(
        dismiss: () -> Unit,
        startingState: WidgetTransientState,
        progressedState: WidgetTransientState,
        parentWidth: Int,
        viewAnimation: ViewAnimationManager,
        state: (WidgetTransientState) -> Unit
    ) {
        dismissWidget = dismiss
        this.timeout = startingState.interactionPhaseTimeout
        this.parentWidth = parentWidth
        this.viewAnimation = viewAnimation
        this.progressedStateCallback = state
        this.startingState = startingState
        this.progressedState = progressedState
        future = executor.scheduleAtFixedRate(Updater(), 0, 1, TimeUnit.SECONDS)
        inflate(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_image_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_image_widget)

        pieTimerViewStub = findViewById(R.id.prediction_pie)
        pieTimerViewStub.layoutResource = R.layout.cross_image
        pieTimerViewStub.inflate()

        updateCrossImage()
        resultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
        questionTextView.layoutParams.width = parentWidth
    }

    private fun updateCrossImage() {
        prediction_followup_image_cross.apply {
            setImageResource(R.mipmap.widget_ic_x)
            setOnClickListener { dismissWidget?.invoke() }
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

    override fun questionUpdated(questionText: String) {
        viewAnimation.addHorizontalSwipeListener(questionTextView.apply {
            text = questionText
            isClickable = true
        }, layout, dismissWidget)
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>
    ) {
        val correctOption = correctOptionWithUserSelection.first
        val userSelectedOption = correctOptionWithUserSelection.second
        initAdapter(voteOptions, correctOption, userSelectedOption)
    }

    private fun initAdapter(voteOptions: List<VoteOption>, correctOption: String?, userSelectedOption: String?) {
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        imageOptionList.layoutManager = linearLayoutManager
        imageAdapter  = ImageAdapter(voteOptions, object : (String?) ->Unit {
            override fun invoke(optionId: String?) {}
        }, parentWidth, context, resultDisplayUtil) {
            if(progressedState.userSelection != null)
                optionSelectedUpdated(progressedState.userSelection)
            updateVoteCount(voteOptions, correctOption, userSelectedOption)
        }
        imageOptionList.adapter = imageAdapter
    }


    private fun updateVoteCount(voteOptions: List<VoteOption>, correctOption: String?, userSelectedOption: String?) {
        viewAnimation.startWidgetTransitionInAnimation {
            resultDisplayUtil.startResultAnimation(correctOption == userSelectedOption, prediction_result,
                {
                    progressedState.resultAnimatorStartPhase = it
                    progressedStateCallback.invoke(progressedState)
                },
                {
                    progressedState.resultAnimationPath = it
                    progressedStateCallback.invoke(progressedState)
                }, startingState)
        }
        voteOptions.forEach { option ->
            val viewOption = imageAdapter?.viewOptions?.get(option.id)
            if (viewOption != null) {
                viewOption.button.setOnClickListener(null)
                viewOption.progressBar.progress = option.answerCount.toInt()
                viewOption.percentageTextView.text = option.answerCount.toString().plus("%")
                resultDisplayUtil.updateViewDrawable(
                    option.id,
                    viewOption.progressBar,
                    viewOption.button,
                    option.answerCount.toInt(),
                    correctOption,
                    userSelectedOption
                )
            }
        }
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {}
    override fun confirmMessageUpdated(confirmMessage: String) {}
}