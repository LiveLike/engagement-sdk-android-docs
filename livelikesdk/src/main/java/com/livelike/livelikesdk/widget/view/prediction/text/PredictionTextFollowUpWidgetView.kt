package com.livelike.livelikesdk.widget.view.prediction.text

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
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.util.AndroidResource.Companion.dpToPx
import com.livelike.livelikesdk.widget.model.VoteOption
import kotlinx.android.synthetic.main.confirm_message.view.*

internal class PredictionTextFollowUpWidgetView :
    PredictionTextWidgetBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    companion object {
        const val correctAnswerLottieFilePath = "correctAnswer"
        const val wrongAnswerLottieFilePath = "wrongAnswer"
    }
    private lateinit var viewAnimation: ViewAnimation
    private var timeout: Long = 7000

    override fun initialize(dismiss : ()->Unit, timeout: Long) {
        super.initialize(dismiss, timeout)
        pieTimerViewStub.layoutResource = R.layout.cross_image
        pieTimerViewStub.inflate()
        val imageView = findViewById<ImageView>(R.id.prediction_followup_image_cross)
        imageView.setImageResource(R.mipmap.widget_ic_x)
        imageView.setOnClickListener { dismissWidget() }
        viewAnimation = ViewAnimation(this)
        this.timeout = timeout
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
            val (percentageDrawable: Int) = provideStyleToButtonAndProgressBar(correctOption, userSelectedOption, button)
            val percentage = voteOptions.single { option ->  option.id == optionId }.votePercentage
            val (progressBar, textViewPercentage) = createResultView(context, percentage.toInt(), percentageDrawable)
            applyConstraintsBetweenProgressBarAndButton(progressBar, button, textViewPercentage)
        }

        lottieAnimationPath = if (hasUserSelectedCorrectOption(correctOption, userSelectedOption))
            correctAnswerLottieFilePath
        else wrongAnswerLottieFilePath

        transitionAnimation()
    }

    private fun provideStyleToButtonAndProgressBar(correctOption: String?, userSelectedOption: String?, button: Button): Pair<Int, String?> {
        val percentageDrawable: Int
        val buttonId = buttonMap[button]
        if (hasUserSelectedCorrectOption(userSelectedOption, correctOption)) {
            if (isCurrentButtonSameAsCorrectOption(correctOption, buttonId)) {
                percentageDrawable = R.drawable.progress_bar_user_correct
                button.background = AppCompatResources.getDrawable(context, R.drawable.button_correct_answer_outline)
            } else {
                percentageDrawable = R.drawable.progress_bar_wrong_option
            }
        } else {
            when {
                isCurrentButtonSameAsCorrectOption(correctOption, buttonId) -> {
                    percentageDrawable = R.drawable.progress_bar_user_correct
                    button.background = AppCompatResources.getDrawable(context, R.drawable.button_correct_answer_outline)
                }
                isCurrentButtonSameAsCorrectOption(userSelectedOption, buttonId) -> {
                    percentageDrawable = R.drawable.progress_bar_user_selection_wrong
                    button.background = AppCompatResources.getDrawable(context, R.drawable.button_wrong_answer_outline)
                }
                else -> {
                    percentageDrawable = R.drawable.progress_bar_wrong_option
                }
            }
        }
        return Pair(percentageDrawable, buttonId)
    }

    private fun hasUserSelectedCorrectOption(userSelectedOption: String?, correctOption: String?) =
        userSelectedOption == correctOption

    private fun isCurrentButtonSameAsCorrectOption(correctOption: String?, buttonId: String?) =
        buttonId == correctOption

    private fun createResultView(context: Context, percentage: Int, percentageDrawable: Int): Pair<ProgressBar, TextView> {
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        addStyleToProgressBar(progressBar, context, percentage, percentageDrawable)

        val textViewPercentage = TextView(context)
        addStyleToShowResultTextView(textViewPercentage, percentage)

        layout.addView(textViewPercentage)
        layout.addView(progressBar)
        return Pair(progressBar, textViewPercentage)
    }

    private fun addStyleToShowResultTextView(textViewPercentage: TextView, percentage: Int) {
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

    private fun addStyleToProgressBar(progressBar: ProgressBar, context: Context, percentage: Int, percentageDrawable: Int) {
        progressBar.apply {
            layoutParams = LayoutParams(
                    0,
                    0)
            elevation = dpToPx(1).toFloat()
            max = 100
            progress = percentage
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

    private fun transitionAnimation() {
        viewAnimation.startWidgetTransitionInAnimation{
            viewAnimation.startResultAnimation(lottieAnimationPath, context, prediction_result)
        }
        Handler().postDelayed(
            { viewAnimation.triggerTransitionOutAnimation { dismissWidget?.invoke() } },
            timeout
        )
    }
}
