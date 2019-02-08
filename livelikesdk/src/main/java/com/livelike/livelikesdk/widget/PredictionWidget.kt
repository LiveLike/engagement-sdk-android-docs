package com.livelike.livelikesdk.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.*
import com.daimajia.easing.Glider
import com.daimajia.easing.Skill
import com.livelike.livelikesdk.LayoutTouchListener
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.prediction_text_widget.view.*

@SuppressLint("ViewConstructor")
class PredictionWidget @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var touchListener: View.OnTouchListener
    private lateinit var parentView: ScrollView
    private val buttonList: ArrayList<Button> = ArrayList()
    private var optionSelected = false
    private val widgetDismissDuration: Long = 100000
    private val widgetOpacityFactor: Float = 0.2f
    private val constraintSet = ConstraintSet()
    private var layout = ConstraintLayout(context, attrs, defStyleAttr)

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
        this.touchListener = LayoutTouchListener(this, parentView)

        addNewlyCreatedButtonsToLayout(options, context)
        applyConstraintsBetweenViews(constraintSet, textView)

        // TODO: Actually start this animation after the bounce effect is completed.
        performTimerAnimation()
        performEasingAnimation()

        prediction_text_widget.setOnTouchListener(touchListener)
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun addNewlyCreatedButtonsToLayout(options: ArrayList<String>, context: Context) {
        options.forEachIndexed { index, element ->
            val button = Button(context)
            buttonList.add(button)
            applyStyle(button, options, index, context, element)
            setOnClickListener(button, context)
            button.setOnTouchListener(touchListener)
            layout.addView(button)
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

    private fun setOnClickListener(button: Button, context: Context) {
        button.setOnClickListener { view ->
            buttonList.forEach { button ->
                optionSelected = true
                // TODO: Move this logic to android state drawables.
                if (button.id == view.id) {
                    button.background = AppCompatResources.getDrawable(context, R.drawable.button_pressed)
                } else
                    button.background = AppCompatResources.getDrawable(context, R.drawable.button_default)
            }
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

    private fun isLastButtonToBeAddedToLayout(buttonNames: ArrayList<String>, index: Int) =
            buttonNames[index] == buttonNames[buttonNames.size - 1]

    private fun performTimerAnimation() {
        bindListenerToTimerAnimation()
        prediction_pie_updater_animation.playAnimation()
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = widgetDismissDuration

        animator.addUpdateListener { animation ->
            prediction_pie_updater_animation.progress = animation.animatedValue as Float
        }
        animator.start()

        bindListenerToConfirmMessageAnimation()
    }

    private fun performEasingAnimation() {
        val heightToReach = this.measuredHeight
        val animatorSet = AnimatorSet()

        // TODO: remove hardcoded start position -400 to something meaningful.
        animatorSet.playTogether(
                Glider.glide(Skill.ElasticEaseOut, 12000f, ObjectAnimator.ofFloat(this,
                        "translationY",
                        -400f,
                        heightToReach.toFloat(),
                        heightToReach.toFloat() / 2, 0f))
        )

        animatorSet.duration = 5000
        animatorSet.start()
    }

    private fun bindListenerToTimerAnimation() {
        prediction_pie_updater_animation.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) { Log.e("Animation:", "start") }
            override fun onAnimationEnd(animation: Animator) {
                Log.e("Animation:", "end")
                if (optionSelected) {
                    prediction_confirm_message_textView.visibility = View.VISIBLE
                    prediction_confirm_message_animation.visibility = View.VISIBLE
                    prediction_confirm_message_animation.playAnimation()
                    performPredictionWidgetFadeOutOperations()
                    Handler().postDelayed({ this@PredictionWidget.visibility = View.INVISIBLE }, widgetDismissDuration)
                } else hideWidget()
            }

            fun performPredictionWidgetFadeOutOperations() {
                buttonList.forEach { button ->
                    disableButtons(button)
                    setViewTranslucent(button)
                }
                setViewTranslucent(prediction_question_textView)
                setViewTranslucent(prediction_pie_updater_animation)
            }

            fun setViewTranslucent(view: View) { view.alpha = widgetOpacityFactor }
            fun disableButtons(button: Button) { button.isEnabled = false }
            override fun onAnimationCancel(animation: Animator) { Log.e("Animation:", "cancel") }
            override fun onAnimationRepeat(animation: Animator) { Log.e("Animation:", "repeat") }
        })
    }

    private fun bindListenerToConfirmMessageAnimation() {
        prediction_confirm_message_animation.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) { Log.e("Animation:", "start") }
            override fun onAnimationEnd(animation: Animator) {
                Log.e("Animation:", "end")
                Handler().postDelayed({ hideWidget() }, 3000)
            }

            override fun onAnimationCancel(animation: Animator) { Log.e("Animation:", "cancel") }
            override fun onAnimationRepeat(animation: Animator) { Log.e("Animation:", "repeat") }
        })
    }

    private fun hideWidget() {
        layout.visibility = View.INVISIBLE
    }
}