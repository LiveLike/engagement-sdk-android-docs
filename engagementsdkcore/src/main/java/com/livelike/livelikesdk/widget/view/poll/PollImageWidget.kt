package com.livelike.livelikesdk.widget.view.poll

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.widget.animation.ViewAnimationManager
import com.livelike.livelikesdk.widget.binding.QuizVoteObserver
import com.livelike.livelikesdk.widget.binding.WidgetObserver
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.adapters.ImageAdapter
import com.livelike.livelikesdk.widget.util.WidgetResultDisplayUtil
import kotlinx.android.synthetic.main.confirm_message.view.confirmMessageTextView
import kotlinx.android.synthetic.main.prediction_image_widget.view.closeButton
import kotlinx.android.synthetic.main.prediction_image_widget.view.imageOptionList
import kotlinx.android.synthetic.main.prediction_image_widget.view.questionTextView

class PollImageWidget : ConstraintLayout, WidgetObserver, QuizVoteObserver {
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimationManager
    private lateinit var resultDisplayUtil: WidgetResultDisplayUtil
    private lateinit var userTapped: () -> Unit
    private lateinit var startingState: WidgetTransientState
    private lateinit var progressedState: WidgetTransientState
    private lateinit var progressedStateCallback: (WidgetTransientState) -> Unit
    private var imageAdapter: ImageAdapter? = null
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    private var fetchResult: (() -> Unit)? = null
    private var selectedOption: String? = null
    private var correctOption: String? = null
    private var timeout = 0L
    private var prevOptionSelectedId = ""
    var parentWidth = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    internal fun initialize(
        dismiss: () -> Unit,
        startingState: WidgetTransientState,
        progressedState: WidgetTransientState,
        fetch: () -> Unit,
        parentWidth: Int,
        viewAnimation: ViewAnimationManager,
        progressedStateCallback: (WidgetTransientState) -> Unit
    ) {
        this.startingState = startingState
        this.progressedState = progressedState
        this.timeout = startingState.widgetTimeout
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

        resultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
        questionTextView.layoutParams.width = parentWidth
    }

    private fun startWidgetAnimation() {
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
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout * 2)
        viewAnimation.startTimerAnimation(pieTimer, properties.widgetTimeout, properties, {
            fetchResult?.invoke()
            disableButtons()
            closeButton.visibility = View.VISIBLE
            closeButton.setOnClickListener { dismissWidget?.invoke() }
        }, {
            progressedState.timerAnimatorStartPhase = it
            progressedStateCallback.invoke(progressedState)
        })
    }

    override fun questionUpdated(questionText: String) {
        viewAnimation.addHorizontalSwipeListener(questionTextView.apply {
            text = questionText
            isClickable = true
            background = AppCompatResources.getDrawable(context, R.drawable.poll_textview_rounded_corner)
        }, layout, dismissWidget)
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>
    ) {
        imageOptionList.adapter?.let {

            Handler(Looper.getMainLooper()).post { updateVoteCount(voteOptions) }
        } ?: run {
            imageAdapter = ImageAdapter(voteOptions, object : (String?) -> Unit {
                override fun invoke(optionId: String?) {
                    userTapped.invoke()
                    optionSelectedCallback.invoke(optionId)
                }
            }, parentWidth, context, resultDisplayUtil) {
                if (progressedState.userSelection != null)
                    optionSelectedUpdated(progressedState.userSelection)
                startWidgetAnimation()
            }
            imageOptionList.adapter = imageAdapter
        }
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        if (prevOptionSelectedId != selectedOption)
            fetchResult?.invoke()
        prevOptionSelectedId = selectedOption ?: ""
        selectedOption = selectedOptionId
        progressedState.userSelection = selectedOptionId
        imageAdapter?.viewOptions?.forEach { (id, viewOption) ->
            if (selectedOptionId == id)
                viewOption.button.background = AppCompatResources.getDrawable(context, R.drawable.button_poll_answer_outline)
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
        voteOptions.forEach { option ->
            val viewOption = imageAdapter?.viewOptions?.get(option.id)
            if (viewOption != null) {
                viewOption.percentageTextView.visibility = View.VISIBLE
                viewOption.percentageTextView.text = option.votePercentage.toString().plus("%")
                resultDisplayUtil.updateViewDrawable(
                    option.id,
                    viewOption.progressBar,
                    viewOption.button,
                    option.votePercentage.toInt(),
                    correctOption,
                    selectedOption, true
                )
            }
        }
    }

    private fun disableButtons() {
        imageAdapter?.viewOptions?.forEach { option ->
            option.value.button.setOnClickListener(null)
        }
    }
}