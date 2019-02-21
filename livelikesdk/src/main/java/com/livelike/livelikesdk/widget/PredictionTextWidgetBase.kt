package com.livelike.livelikesdk.widget

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.*
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.livelike.livelikesdk.LayoutTouchListener
import com.livelike.livelikesdk.animation.AnimationEaseInterpolator
import com.livelike.livelikesdk.animation.AnimationHandler
import java.util.*
import com.livelike.livelikesdk.R

open class PredictionTextWidgetBase : ConstraintLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    protected val timerDuration: Long = 7000
    protected val widgetShowingDurationAfterConfirmMessage: Long = 3000
    protected val widgetOpacityFactor: Float = 0.2f
    protected var optionSelected = false
    protected val constraintSet = ConstraintSet()
    protected var layout = ConstraintLayout(context, null, 0)
    protected val buttonList: ArrayList<Button> = ArrayList()
    protected val animationHandler = AnimationHandler()
    protected lateinit var pieTimerViewStub : ViewStub

    init {
        inflate(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
                .inflate(R.layout.prediction_text_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_text_widget)
        val options = arrayListOf("player 3", "player 4", "player 5")

        val textView = findViewById<TextView>(R.id.prediction_question_textView)

        addNewlyCreatedButtonsToLayout(options, context)
        applyConstraintsBetweenViews(constraintSet, textView)

        pieTimerViewStub = findViewById(R.id.prediction_pie)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addNewlyCreatedButtonsToLayout(options: ArrayList<String>, context: Context) {
        options.forEachIndexed { index, element ->
            val button = Button(context)
            buttonList.add(button)
            applyStyle(button, options, index, context, element)
            /*button.setOnTouchListener(LayoutTouchListener(this, parentView) {
                performClickAction(it, context)
            })*/
            layout.addView(button)
        }
    }

    private fun applyStyle(button: Button,
                           buttonNames: ArrayList<String>,
                           buttonIndex: Int,
                           context: Context,
                           buttonText: String) {
        button.apply {
            background = if (isLastButtonToBeAddedToLayout(buttonNames, buttonIndex))
                AppCompatResources.getDrawable(context, R.drawable.bottom_rounded_corner)
            else
                AppCompatResources.getDrawable(context, R.drawable.button_default)
            setTextColor(ContextCompat.getColor(context, R.color.text_color))
            text = buttonText
            layoutParams = ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            elevation = dpToPx(4).toFloat()
            gravity = Gravity.START; Gravity.CENTER_VERTICAL
            id = View.generateViewId()
            typeface = ResourcesCompat.getFont(context, R.font.titillium_semibold)
            textSize = 15f
        }
    }

    private fun performClickAction(it: View?, context: Context) {
        buttonList.forEach { button ->
            optionSelected = true
            // TODO: Move this logic to android state drawables.
            if (button.id == it?.id) {
                Toast.makeText(context, "selected option ${button.text} ", Toast.LENGTH_LONG).show()
                // Here we get the selected option which will used for follow up widget.
                button.background = AppCompatResources.getDrawable(context, R.drawable.button_pressed)
            } else
                button.background = AppCompatResources.getDrawable(context, R.drawable.button_default)
        }
    }

    private fun applyConstraintsBetweenViews(constraintSet: ConstraintSet,
                                             textView: TextView) {
        constraintSet.clone(layout)
        val viewIds = ArrayList<Int>()
        val viewWeights = ArrayList<Float>()

        val weightToBeAppliedToEachView = 1.0f
        viewWeights.add(weightToBeAppliedToEachView)
        viewIds.add(textView.id)

        for (button in buttonList) {
            viewIds.add(button.id)
            viewWeights.add(weightToBeAppliedToEachView)
        }
        constraintSet.setVerticalBias(textView.id, 0f)
        constraintSet.createVerticalChain(
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                viewIds.toIntArray(),
                viewWeights.toFloatArray(),
                ConstraintSet.CHAIN_PACKED)

        constraintSet.applyTo(layout)
    }

    private fun isLastButtonToBeAddedToLayout(buttonNames: ArrayList<String>, index: Int) =
            buttonNames[index] == buttonNames[buttonNames.size - 1]

    // Would have to think more on how to not use hard coded values. I think once we have more easing
    // functions to use and how we layout widget and chat we can think of these values more.
    protected fun startEasingAnimation(animationHandler: AnimationHandler) {
        val heightToReach = this.measuredHeight.toFloat()

        // TODO: remove hardcoded start position -400 to something meaningful.
        val animator = ObjectAnimator.ofFloat(this,
                "translationY",
                -400f,
                heightToReach,
                heightToReach / 2, 0f)

        val animationDuration = 5000f
        animationHandler.createAnimationEffectWith(
                AnimationEaseInterpolator.Ease.EaseOutElastic,
                animationDuration,
                animator)
    }

    protected fun dpToPx(dp: Int): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

}