package com.livelike.livelikesdk.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.support.constraint.ConstraintSet
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.prediction_text_widget.view.*

// Note: Need to have presenter and model from this.
// TODO: Refactor as we deal with user interactions. No business logic should be present in this class.
@SuppressLint("ViewConstructor")
class PredictionTextFollowUpWidgetView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : PredictionTextWidgetBase(context, attrs, defStyleAttr) {
    val result : LinkedHashMap<CharSequence, Int> = linkedMapOf(
            buttonList[0].text to 50,
            buttonList[1].text to 70,
            buttonList[2].text to 20)

    init {
        buttonList.forEachIndexed { index, button ->
            if ( index == 1 )
                button.background = AppCompatResources.getDrawable(context, R.drawable.correct_answer_outline)

            button.setOnTouchListener(null)
            val (progressBar, textViewPercentage) = createResultView(context, button)
            applyConstraintsBetweenProgressBarAndButton(progressBar, button, textViewPercentage)
            startEasingAnimation(animationHandler)
            prediction_result.visibility = View.VISIBLE
            prediction_result.playAnimation()
            prediction_result.repeatCount = 10
        }
    }

    private fun createResultView(context: Context, button: Button): Pair<ProgressBar, TextView> {
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        addStyleToProgressBar(progressBar, context, button)

        val textViewPercentage = TextView(context)
        addStyleToShowResultTextView(textViewPercentage, button)

        layout.addView(textViewPercentage)
        layout.addView(progressBar)
        return Pair(progressBar, textViewPercentage)
    }

    private fun addStyleToShowResultTextView(textViewPercentage: TextView, button: Button) {
        textViewPercentage.apply {
            setTextColor(Color.WHITE)
            text = result[button.text].toString() + "%"
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

    private fun addStyleToProgressBar(progressBar: ProgressBar, context: Context, button: Button) {
        progressBar.apply {
            layoutParams = LayoutParams(
                    0,
                    0)
            elevation = dpToPx(2).toFloat()
            max = 100
            progress = result[button.text]!!
            isIndeterminate = false
            Log.d("Widget", "Abhishek ${button.text}")
            if (button.text == "player 4") {
                Log.d("Widget", "Abhishek ${button.text}")
                progressDrawable = AppCompatResources.getDrawable(context, R.drawable.progress_bar)
            }
              else  progressDrawable = AppCompatResources.getDrawable(context, R.drawable.progress_bar_looser)
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

}