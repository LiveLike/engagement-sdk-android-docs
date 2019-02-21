package com.livelike.livelikesdk.widget.view

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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.Button
import android.widget.TextView
import com.livelike.livelikesdk.animation.AnimationEaseInterpolator
import com.livelike.livelikesdk.animation.AnimationHandler
import com.livelike.livelikesdk.widget.model.WidgetOptionsData
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.binding.Observer
import kotlinx.android.synthetic.main.prediction_text_widget.view.prediction_question_textView
import java.util.Random
import kotlin.collections.ArrayList

open class PredictionTextWidgetBase : ConstraintLayout, Observer {
    protected val timerDuration: Long = 7000
    protected val widgetShowingDurationAfterConfirmMessage: Long = 3000
    protected val widgetOpacityFactor: Float = 0.2f
    protected val constraintSet = ConstraintSet()
    protected val buttonList: ArrayList<Button> = ArrayList()
    protected val animationHandler = AnimationHandler()
    protected var optionSelected = false
    protected var layout = ConstraintLayout(context, null, 0)
    protected var lottieAnimationPath = ""
    protected lateinit var pieTimerViewStub : ViewStub

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init { inflate(context) }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
                .inflate(R.layout.prediction_text_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_text_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
    }

    override fun questionUpdated(questionText: String) {
        prediction_question_textView.text = questionText
    }

    override fun optionListUpdated(optionList: Map<String, Long>, optionSelectedCallback: (CharSequence?) -> Unit, correctOptionWithUserSelection: Pair<String?, String?>) {
        val textView = findViewById<TextView>(R.id.prediction_question_textView)
        addNewlyCreatedButtonsToLayout(context, ArrayList(optionList.keys), optionSelectedCallback)
        applyConstraintsBetweenViews(constraintSet, textView)
    }

    override fun optionSelectedUpdated(selectedOption: WidgetOptionsData) {
        buttonList.single { button ->
            button.text == selectedOption.description
        }.background = AppCompatResources.getDrawable(context, R.drawable.button_pressed)

        buttonList.filterNot { button ->
            button.text == selectedOption.description
        }.forEach{ button ->
            button.background = AppCompatResources.getDrawable(context, R.drawable.button_default)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addNewlyCreatedButtonsToLayout(
        context: Context,
        optionList: List<String>,
        optionSelectedCallback: (CharSequence?) -> Unit) {
        optionList.forEachIndexed { index, element ->
            val button = Button(context)
            buttonList.add(button)
            applyStyle(button, optionList, index, context, element)
            button.setOnClickListener {
                optionSelected = true
                optionSelectedCallback(button.text)
            }
            layout.addView(button)
        }
    }

    private fun applyStyle(button: Button,
                           buttonNames: List<String>?,
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

    private fun isLastButtonToBeAddedToLayout(buttonNames: List<String>?, index: Int) =
            buttonNames!![index] == buttonNames[buttonNames.size - 1]

    // Would have to think more on how to not use hard coded values. I think once we have more easing
    // functions to use and how we layout widget and chat we can think of these values more.
    protected fun startEasingAnimation(
        animationHandler: AnimationHandler,
        ease: AnimationEaseInterpolator.Ease,
        animator: ObjectAnimator) {
        val animationDuration = 1000f
        when(ease)  {
            AnimationEaseInterpolator.Ease.EaseOutElastic -> {
                animationHandler.createAnimationEffectWith(
                    ease,
                    animationDuration,
                    animator)
            }
            AnimationEaseInterpolator.Ease.EaseOutQuad -> {
                animationHandler.createAnimationEffectWith(
                    ease,
                    animationDuration,
                    animator
                )
            }

            else -> {}
        }
    }

    protected fun dpToPx(dp: Int): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    protected fun selectRandomLottieAnimation(path: String): String? {
        val asset = context?.assets
        val assetList = asset?.list(path)
        val random = Random()
        return if (assetList!!.isNotEmpty()) {
            val emojiIndex = random.nextInt(assetList.size)
            assetList[emojiIndex]
        } else return null
    }
}