package com.livelike.livelikesdk.widget.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.support.constraint.ConstraintSet
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.AnimationEaseInterpolator
import com.livelike.livelikesdk.widget.model.VoteOption
import kotlinx.android.synthetic.main.prediction_text_widget.view.prediction_result

class PredictionTextFollowUpWidgetView : PredictionTextWidgetBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, dismiss: () -> Unit) : super(context, attrs, defStyleAttr, dismiss)

    companion object {
        const val correctAnswerLottieFilePath = "correctAnswer"
        const val wrongAnswerLottieFilePath = "wrongAnswer"
    }

    init {
        pieTimerViewStub.layoutResource = R.layout.cross_image
        pieTimerViewStub.inflate()
        val imageView = findViewById<ImageView>(R.id.prediction_followup_image_cross)
        imageView.setImageResource(R.mipmap.widget_ic_x)
        imageView.setOnClickListener { dismissWidget() }
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>) {
        super.optionListUpdated(voteOptions, optionSelectedCallback, correctOptionWithUserSelection)

        val correctOption = correctOptionWithUserSelection.first
        val userSelectedOption = correctOptionWithUserSelection.second

        buttonMap.forEach{(button, optionId) ->
            button.setOnClickListener(null)
            val (percentageDrawable: Int, buttonText) = provideStyleToButtonAndProgressBar(correctOption, userSelectedOption, button)
            val percentage = voteOptions.single { option ->  option.id == optionId }.votePercentage
            val (progressBar, textViewPercentage) = createResultView(context, percentage, percentageDrawable)
            applyConstraintsBetweenProgressBarAndButton(progressBar, button, textViewPercentage)
        }

        lottieAnimationPath = if (hasUserSelectedCorrectOption(correctOption, userSelectedOption))
            correctAnswerLottieFilePath
        else wrongAnswerLottieFilePath

        triggerTransitionInAnimation()
        triggerTransitionOutAnimation()
    }

    private fun provideStyleToButtonAndProgressBar(correctOption: String?, userSelectedOption: String?, button: Button): Pair<Int, String?> {
        val percentageDrawable: Int
        val buttonId = buttonMap[button]
        if (hasUserSelectedCorrectOption(userSelectedOption, correctOption)) {
            if (isCurrentButtonSameAsCorrectOption(correctOption, buttonId)) {
                percentageDrawable = R.drawable.progress_bar
                selectedOptionCorrect(buttonId.toString())
            } else {
                percentageDrawable = R.drawable.progress_bar_looser
            }
        } else {
            when {
                isCurrentButtonSameAsCorrectOption(correctOption, buttonId) -> {
                    percentageDrawable = R.drawable.progress_bar
                    selectedOptionCorrect(buttonId.toString())
                }
                isCurrentButtonSameAsCorrectOption(userSelectedOption, buttonId) -> {
                    percentageDrawable = R.drawable.progress_bar_wrong
                    selectedOptionIncorrect(buttonId.toString())
                }
                else -> percentageDrawable = R.drawable.progress_bar_looser
            }
        }
        return Pair(percentageDrawable, buttonId)
    }

    private fun hasUserSelectedCorrectOption(userSelectedOption: String?, correctOption: String?) =
        userSelectedOption == correctOption

    private fun isCurrentButtonSameAsCorrectOption(correctOption: String?, buttonId: String?) =
        buttonId == correctOption

    private fun selectedOptionCorrect(optionDescription: String) {
        outlineButton(optionDescription, R.drawable.correct_answer_outline)
    }

    private fun selectedOptionIncorrect(optionDescription: String) {
        outlineButton(optionDescription, R.drawable.wrong_answer_outline)
    }

    private fun outlineButton(optionDescription: String, drawableId: Int) {
        val singleOrNull = buttonList.singleOrNull { button ->
            button.text == optionDescription
        }
        singleOrNull?.background = AppCompatResources.getDrawable(context, drawableId)
    }


    private fun createResultView(context: Context, percentage: Long?, percentageDrawable: Int): Pair<ProgressBar, TextView> {
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        addStyleToProgressBar(progressBar, context, percentage, percentageDrawable)

        val textViewPercentage = TextView(context)
        addStyleToShowResultTextView(textViewPercentage, percentage)

        layout.addView(textViewPercentage)
        layout.addView(progressBar)
        return Pair(progressBar, textViewPercentage)
    }

    private fun addStyleToShowResultTextView(textViewPercentage: TextView, percentage: Long?) {
        textViewPercentage.apply {
            setTextColor(Color.WHITE)
            text = percentage.toString().plus("%")
            layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            elevation = dpToPx(8).toFloat()
            val padding = dpToPx(16)
            setPadding(padding, padding, padding, padding)
            gravity = Gravity.START; Gravity.CENTER_VERTICAL
            id = View.generateViewId()
        }
    }

    private fun addStyleToProgressBar(progressBar: ProgressBar, context: Context, percentage: Long?, percentageDrawable: Int) {
        progressBar.apply {
            layoutParams = LayoutParams(
                    0,
                    0)
            elevation = dpToPx(1).toFloat()
            max = 100
            progress = percentage!!.toInt()
            isIndeterminate = false
            progressDrawable = AppCompatResources.getDrawable(context, percentageDrawable)
            id = View.generateViewId()
        }
    }

    private fun applyConstraintsBetweenProgressBarAndButton(progressBar: ProgressBar,
                                                            button: Button,
                                                            textViewPercentage: TextView) {
        constraintSet.clone(layout)

        constraintSet.connect(textViewPercentage.id, ConstraintSet.START, progressBar.id, ConstraintSet.END, 0)
        constraintSet.connect(textViewPercentage.id, ConstraintSet.END, button.id, ConstraintSet.END, 0)
        constraintSet.connect(textViewPercentage.id, ConstraintSet.TOP, button.id, ConstraintSet.TOP, 0)
        constraintSet.connect(textViewPercentage.id, ConstraintSet.BOTTOM, button.id, ConstraintSet.BOTTOM, 0)
        constraintSet.setHorizontalBias(textViewPercentage.id, 0.5f)
        constraintSet.setVerticalBias(textViewPercentage.id, 1f)

        constraintSet.connect(progressBar.id, ConstraintSet.START, button.id, ConstraintSet.START, dpToPx(8))
        constraintSet.connect(progressBar.id, ConstraintSet.END, button.id, ConstraintSet.END, dpToPx(8))
        constraintSet.connect(progressBar.id, ConstraintSet.TOP, button.id, ConstraintSet.TOP, dpToPx(8))
        constraintSet.connect(progressBar.id, ConstraintSet.BOTTOM, button.id, ConstraintSet.BOTTOM, dpToPx(8))
        constraintSet.setHorizontalBias(progressBar.id, 0f)
        constraintSet.constrainPercentWidth(progressBar.id, 0.9f)

        constraintSet.applyTo(layout)
    }

    private fun triggerTransitionInAnimation() {
        val heightToReach = this.measuredHeight.toFloat()
        val animator = ObjectAnimator.ofFloat(this,
                "translationY",
                -400f,
                heightToReach,
                heightToReach / 2, 0f)

        animationHandler.bindListenerToAnimationView(animator) {
            val lottieAnimation = selectRandomLottieAnimation(lottieAnimationPath)
            if (lottieAnimation != null)
                prediction_result.setAnimation("$lottieAnimationPath/$lottieAnimation")
            prediction_result.visibility = View.VISIBLE
            prediction_result.playAnimation()
        }

        startEasingAnimation(animationHandler, AnimationEaseInterpolator.Ease.EaseOutElastic, animator)
    }

    // TODO: EaseOutQuad needs to be EaseOutQuart. Current library does not support it so we will have to switch soon to
    // https://github.com/MasayukiSuda/EasingInterpolator

    private fun triggerTransitionOutAnimation() {
        Handler().postDelayed({
            // TODO: Get rid of hardcoded value once we have minimun viewable area defined.
            val animator = ObjectAnimator.ofFloat(this,
                    "translationY",
                    0f, -dpToPx(250).toFloat())
            startEasingAnimation(animationHandler, AnimationEaseInterpolator.Ease.EaseOutQuad, animator)
        }, resources.getInteger(R.integer.prediction_widget_follow_transition_out_in_milliseconds).toLong())
    }
}
