package com.livelike.livelikesdk.widget.view.prediction.image

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
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.adapters.ImageAdapter
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil
import kotlinx.android.synthetic.main.confirm_message.view.confirmMessageTextView
import kotlinx.android.synthetic.main.confirm_message.view.prediction_confirm_message_animation
import kotlinx.android.synthetic.main.pie_timer.view.prediction_pie_updater_animation
import kotlinx.android.synthetic.main.prediction_image_widget.view.imageOptionList
import kotlinx.android.synthetic.main.prediction_image_widget.view.questionTextView


internal class PredictionImageQuestionWidget : ConstraintLayout, WidgetObserver {
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimationManager
    private lateinit var startingState: WidgetTransientState
    private lateinit var progressedStateCallback: (WidgetTransientState) -> Unit
    private lateinit var progressedState: WidgetTransientState
    lateinit var resultDisplayUtil: WidgetResultDisplayUtil
    private val widgetOpacityFactor: Float = 0.2f
    private var optionSelected = false
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    var parentWidth = 0
    private var imageAdapter : ImageAdapter? = null
    lateinit var userTapped: () -> Unit
    private var timeout = 0L
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun initialize(
        dismiss: () -> Unit,
        timeout: Long,
        startingState: WidgetTransientState,
        progressedState: WidgetTransientState,
        parentWidth: Int,
        viewAnimation: ViewAnimationManager,
        state: (WidgetTransientState) -> Unit
    ) {
        dismissWidget = dismiss
        this.viewAnimation = viewAnimation
        this.startingState = startingState
        this.parentWidth = parentWidth
        this.progressedState = progressedState
        this.progressedStateCallback = state

        inflate(context, timeout)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context, timeout: Long) {
        this.timeout = timeout
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_image_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_image_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
        pieTimerViewStub.layoutResource = R.layout.pie_timer


        resultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout * 2)
        questionTextView.layoutParams.width = parentWidth
    }

    private fun startWidgetAnimation() {
        val pieTimer = pieTimerViewStub.inflate()
        if (startingState.timerAnimatorStartPhase != 0f && startingState.resultAnimatorStartPhase == 0f) {
            startPieTimer(pieTimer, timeout)
        } else if (startingState.timerAnimatorStartPhase != 0f && startingState.resultAnimatorStartPhase != 0f) {
            showConfirmMessage()
            performPredictionWidgetFadeOutOperations()
        } else viewAnimation.startWidgetTransitionInAnimation {
            startPieTimer(pieTimer, timeout)
        }
    }
    private fun startPieTimer(pieTimer: View, timeout: Long) {
        viewAnimation.startTimerAnimation(pieTimer, timeout, startingState, {
            if (optionSelected) {
                viewAnimation.showConfirmMessage(
                    confirmMessageTextView,
                    prediction_confirm_message_animation,
                    {},
                    {
                        progressedState.resultAnimatorStartPhase = it
                        progressedStateCallback.invoke(progressedState)
                    },
                    {
                        progressedState.resultAnimationPath = it
                        progressedStateCallback.invoke(progressedState)
                    },
                    startingState
                )
                performPredictionWidgetFadeOutOperations()
            }
        }, {
            progressedState.timerAnimatorStartPhase = it
            progressedStateCallback.invoke(progressedState)
        })
    }

    private fun showConfirmMessage() {
        viewAnimation.showConfirmMessage(
            confirmMessageTextView,
            prediction_confirm_message_animation,
            {},
            {
                progressedState.resultAnimatorStartPhase = it
                progressedStateCallback.invoke(progressedState)
            },
            {
                progressedState.resultAnimationPath = it
                progressedStateCallback.invoke(progressedState)
            },
            startingState
        )
    }

    private fun performPredictionWidgetFadeOutOperations() {
        imageAdapter?.viewOptions?.forEach { (id, viewOption) ->
            disableButtons(viewOption.button)
            viewOption.button.setTranslucent()
        }
        questionTextView.setTranslucent()
        prediction_pie_updater_animation.setTranslucent()
    }

    private fun View.setTranslucent() {
        this.alpha = widgetOpacityFactor
    }

    private fun disableButtons(button: View) {
        button.isEnabled = false
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
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        imageOptionList.layoutManager = linearLayoutManager
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

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        optionSelected = true
        progressedState.userSelection = selectedOptionId
        imageAdapter?.viewOptions?.forEach { (id, viewOption) ->
            if (selectedOptionId == id)
                viewOption.button.background = AppCompatResources.getDrawable(context, R.drawable.prediction_button_pressed)
            else viewOption.button.background = AppCompatResources.getDrawable(context, R.drawable.button_rounded_corners)
        }
    }

    override fun confirmMessageUpdated(confirmMessage: String) {
        confirmMessageTextView.text = confirmMessage
    }

    fun userTappedCallback(userTapped: () -> Unit) {
        this.userTapped = userTapped
    }
}