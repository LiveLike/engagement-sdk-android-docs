package com.livelike.livelikesdk.widget

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.*
import android.widget.*
import android.widget.Toast.LENGTH_LONG
import com.livelike.livelikesdk.LayoutTouchListener
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.AnimationEaseInterpolator
import com.livelike.livelikesdk.animation.AnimationHandler
import kotlinx.android.synthetic.main.pie_timer.view.*
import kotlinx.android.synthetic.main.prediction_text_widget.view.*

// Note: Need to have presenter and model from this.
// TODO: Refactor as we deal with user interactions. No business logic should be present in this class.
@SuppressLint("ViewConstructor")
class PredictionTextWidgetView : ConstraintLayout {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var parentView: ScrollView
    private val buttonList: ArrayList<Button> = ArrayList()
    private val timerDuration: Long = 7000
    private val widgetShowingDurationAfterConfirmMessage: Long = 3000
    private val widgetOpacityFactor: Float = 0.2f
    private val constraintSet = ConstraintSet()
    private var layout = ConstraintLayout(context, null, 0)
    private var optionSelected = false

    init {
        inflate(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
                .inflate(R.layout.prediction_text_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_text_widget)
        parentView = findViewById(R.id.prediction_text_widget_scroll_view)
        val options = arrayListOf("player 3", "player 4", "player 5")

        val textView = findViewById<TextView>(R.id.prediction_question_textView)

        addNewlyCreatedButtonsToLayout(options, context)
        applyConstraintsBetweenViews(constraintSet, textView)

        val pieTimerViewStub = findViewById<ViewStub>(R.id.prediction_pie)
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()

        prediction_text_widget.setOnTouchListener(LayoutTouchListener(this, parentView))

        startWidgetAnimation(pieTimer, AnimationHandler())
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addNewlyCreatedButtonsToLayout(options: ArrayList<String>, context: Context) {
        options.forEachIndexed { index, element ->
            val button = Button(context)
            buttonList.add(button)
            applyStyle(button, options, index, context, element)
            button.setOnTouchListener(LayoutTouchListener(this, parentView) {
                performClickAction(it, context)
            })
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
                Toast.makeText(context, "selected option ${button.text} ", LENGTH_LONG).show()
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

    private fun onTimerAnimationCompleted(animationHandler: AnimationHandler) {
        if (optionSelected) {
            prediction_confirm_message_textView.visibility = View.VISIBLE
            prediction_confirm_message_animation.visibility = View.VISIBLE
            animationHandler.startAnimation(
                    prediction_confirm_message_animation,
                    {hideWidget()},
                    widgetShowingDurationAfterConfirmMessage)
            performPredictionWidgetFadeOutOperations()
        } else hideWidget()
    }

    private fun hideWidget() { prediction_text_widget_scroll_view.visibility = View.INVISIBLE }

    private fun performPredictionWidgetFadeOutOperations() {
        buttonList.forEach { button ->
            disableButtons(button)
            button.setTranslucent()
        }
        prediction_question_textView.setTranslucent()
        prediction_pie_updater_animation.setTranslucent()
    }

    private fun View.setTranslucent() {
        this.alpha = widgetOpacityFactor
    }

    private fun disableButtons(button: Button) { button.isEnabled = false }

    private fun startWidgetAnimation(pieTimer: View, animationHandler: AnimationHandler) {
        startEasingAnimation(animationHandler)
        startTimerAnimation(pieTimer, animationHandler)
    }

    private fun startTimerAnimation(pieTimer: View, animationHandler: AnimationHandler) {
        animationHandler.startAnimation(
                pieTimer.findViewById(R.id.prediction_pie_updater_animation),
                { onTimerAnimationCompleted(animationHandler) },
                timerDuration)
    }

    // Would have to think more on how to not use hard coded values. I think once we have more easing
    // functions to use and how we layout widget and chat we can think of these values more.
    private fun startEasingAnimation(animationHandler: AnimationHandler) {
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
}