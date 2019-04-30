package com.livelike.livelikesdk.widget.view.quiz

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.binding.QuizVoteObserver
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.adapters.ImageAdapter
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.prediction_image_widget.view.*

class QuizImageWidget : ConstraintLayout, WidgetObserver, QuizVoteObserver {
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimationManager
    private lateinit var resultDisplayUtil: WidgetResultDisplayUtil
    private lateinit var userTapped: () -> Unit
    private lateinit var startingState: WidgetTransientState
    private lateinit var progressedState: WidgetTransientState
    private lateinit var progressedStateCallback: (WidgetTransientState) -> Unit
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    private var fetchResult: (() -> Unit)? = null
    private var selectedOption : String? = null
    private var correctOption: String? = null
    private var animationInCompleted = false
    private var timeout = 0L
    private var imageAdapter : ImageAdapter? = null
    var parentWidth = 0
    
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    internal fun initialize(dismiss : ()->Unit,
                            startingState: WidgetTransientState,
                            progressedState: WidgetTransientState,
                            fetch: () -> Unit,
                            parentWidth: Int,
                            viewAnimation: ViewAnimationManager,
                            progressedStateCallback: (WidgetTransientState) -> Unit) {
        this.startingState = startingState
        this.progressedState = progressedState
        this.timeout = startingState.timeout
        this.dismissWidget = dismiss
        this.fetchResult = fetch
        this.viewAnimation = viewAnimation
        this.parentWidth = parentWidth
        this.progressedStateCallback = progressedStateCallback
        inflate(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_image_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_image_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)

        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        imageOptionList.layoutManager = linearLayoutManager

        this.visibility = View.INVISIBLE
        resultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
        questionTextView.layoutParams.width = parentWidth
    }

    private fun startWidgetAnimation() {
        if(animationInCompleted)
            return
        animationInCompleted = true
        this.visibility = View.VISIBLE
        when {
            isWidgetDisplayedFirstTime(startingState) -> viewAnimation.startWidgetTransitionInAnimation {
                pieTimerViewStub.layoutResource = R.layout.pie_timer
                val pieTimer = pieTimerViewStub.inflate()
                startPieTimer(pieTimer, startingState)
            }
            isWidgetRestoredFromQuestionPhase(startingState) -> {
                pieTimerViewStub.layoutResource = R.layout.pie_timer
                val pieTimer = pieTimerViewStub.inflate()
                startPieTimer(pieTimer, startingState)
            }
            else -> {
                pieTimerViewStub.layoutResource = R.layout.cross_image
                pieTimerViewStub.inflate()
            }
        }
    }

    private fun isWidgetRestoredFromQuestionPhase(properties: WidgetTransientState) =
        properties.timerAnimatorStartPhase > 0f && properties.resultAnimatorStartPhase == 0f

    private fun isWidgetDisplayedFirstTime(properties: WidgetTransientState) =
        properties.timerAnimatorStartPhase == 0f

    private fun startPieTimer(pieTimer: View, properties: WidgetTransientState) {
        viewAnimation.startTimerAnimation(pieTimer, properties.timeout, properties, {
            fetchResult?.invoke()
        }, {
            progressedState.timerAnimatorStartPhase = it
            progressedStateCallback.invoke(progressedState)
        })
    }

    override fun questionUpdated(questionText: String) {
        viewAnimation.addHorizontalSwipeListener(questionTextView.apply {
            text = questionText
            isClickable = true
            background = AppCompatResources.getDrawable(context, R.drawable.quiz_textview_rounded_corner)
        }, layout, dismissWidget)
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>
    ) {
        imageOptionList.adapter?.let {
            if (correctOptionWithUserSelection.first != null && correctOptionWithUserSelection.second != null)
                updateVoteCount(voteOptions)
        } ?: run {
            imageAdapter = ImageAdapter(voteOptions, object : (String?) ->Unit {
                override fun invoke(optionId: String?) {
                    userTapped.invoke()
                    optionSelectedCallback.invoke(optionId)
                }
            }, parentWidth, context, resultDisplayUtil) {
                if(progressedState.userSelection != null)
                    optionSelectedUpdated(progressedState.userSelection)
                startWidgetAnimation()
            }
            imageOptionList.adapter = imageAdapter
        }
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        selectedOption = selectedOptionId
        progressedState.userSelection = selectedOptionId
        imageAdapter?.viewOptions?.forEach { (id, viewOption) ->
            if (selectedOptionId == id)
                viewOption.button.background = AppCompatResources.getDrawable(context, R.drawable.quiz_button_pressed)
            else viewOption.button.background = AppCompatResources.getDrawable(context, R.drawable.button_rounded_corners)
        }
    }

    override fun confirmMessageUpdated(confirmMessage: String) {
        confirmMessageTextView.text = confirmMessage
    }

    fun userTappedCallback(userTapped: () -> Unit) {
        this.userTapped = userTapped
    }

    override fun updateVoteCount(voteOptions: List<VoteOption>) {
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout)
        correctOption = imageAdapter?.correctOption
        resultDisplayUtil.startResultAnimation(
            correctOption == selectedOption, prediction_result,
            {
                progressedState.resultAnimatorStartPhase = it
                progressedStateCallback.invoke(progressedState)
            },
            {
                progressedState.resultAnimationPath = it
                progressedStateCallback.invoke(progressedState)
            }, startingState
        )
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
                    selectedOption
                )
            }
        }
    }
}