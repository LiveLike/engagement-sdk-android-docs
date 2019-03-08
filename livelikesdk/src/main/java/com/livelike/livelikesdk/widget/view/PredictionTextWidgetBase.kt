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
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.easing.AnimationEaseInterpolator
import com.livelike.livelikesdk.animation.AnimationHandler
import com.livelike.livelikesdk.binding.WidgetObserver

import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.SwipeDismissTouchListener
import com.livelike.livelikesdk.widget.model.VoteOption
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.prediction_text_widget.view.*
import java.util.*
import kotlin.collections.ArrayList

open class PredictionTextWidgetBase : ConstraintLayout, WidgetObserver {
    protected val timerDuration: Long = 7000
    protected val widgetShowingDurationAfterConfirmMessage: Long = 3000
    protected val widgetOpacityFactor: Float = 0.2f
    protected val constraintSet = ConstraintSet()
    protected val buttonList: ArrayList<Button> = ArrayList()
    protected val animationHandler = AnimationHandler()
    protected val buttonMap = mutableMapOf<Button, String>()
    protected var optionSelected = false
    protected var layout = ConstraintLayout(context, null, 0)
    protected var lottieAnimationPath = ""
    protected lateinit var pieTimerViewStub : ViewStub
    private var dismissWidget :  (() -> Unit)? = null
    private lateinit var userTapped : () -> Unit

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, dismiss: () -> Unit) : super(context, attrs, defStyleAttr) {
        dismissWidget = dismiss
    }

    init { inflate(context) }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
                .inflate(R.layout.prediction_text_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_text_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
    }

    fun userTappedCallback(userTapped: () -> Unit) {
        this.userTapped = userTapped
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun questionUpdated(questionText: String) {
        addHorizontalSwipeListener(prediction_question_textView.apply {
            text = questionText
            isClickable = true
        })
    }

    override fun confirmMessageUpdated(confirmMessage: String) {
        prediction_confirm_message_textView.text = confirmMessage
    }

    override fun optionListUpdated(voteOptions: List<VoteOption>, optionSelectedCallback: (String?) -> Unit, correctOptionWithUserSelection: Pair<String?, String?>) {
        val textView = findViewById<TextView>(R.id.prediction_question_textView)
        addNewlyCreatedButtonsToLayout(context, voteOptions, optionSelectedCallback)
        applyConstraintsBetweenViews(constraintSet, textView)
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        buttonMap.forEach { (button, id) ->
            if (selectedOptionId == id)
                button.background = AppCompatResources.getDrawable(context, R.drawable.button_pressed)
            else button.background = AppCompatResources.getDrawable(context, R.drawable.button_default)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addNewlyCreatedButtonsToLayout(
        context: Context,
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit) {
        voteOptions.forEachIndexed{ index, option ->
            val button = Button(context)
            buttonList.add(button)
            if (option.id != null) {
                applyStyle(button, option.description , context, voteOptions, index)
                buttonMap[button] = option.id
                button.setOnClickListener {
                    optionSelected = true
                    button.text.also {
                        logDebug { "Option selected: $it" }
                        val selectedButton = buttonMap[button]
                        optionSelectedCallback(selectedButton)
                        userTapped.invoke()
                    }
                }
                addHorizontalSwipeListener(button)
            }
            layout.addView(button)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addHorizontalSwipeListener(view: View) {
        view.setOnTouchListener(object : SwipeDismissTouchListener(layout,
            null, object : DismissCallbacks {
                override fun canDismiss(token: Any?) = true
                override fun onDismiss(view: View?, token: Any?) {
                    animationHandler.cancelAnimation()
                    layout.removeAllViewsInLayout()
                    dismissWidget()
                }
            }) {})
    }

    protected fun dismissWidget() {
        dismissWidget?.invoke()
    }

    private fun applyStyle(button: Button,
                           buttonText: String,
                           context: Context,
                           voteOptions: List<VoteOption>,
                           index: Int) {
        button.apply {
            background = if (isLastButtonToBeAddedToLayout(voteOptions, index)) {
                AppCompatResources.getDrawable(context, R.drawable.bottom_rounded_corner)
            } else AppCompatResources.getDrawable(context, R.drawable.button_default)
            setTextColor(ContextCompat.getColor(context, R.color.text_color))
            text = buttonText
            layoutParams = LayoutParams(
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

    private fun isLastButtonToBeAddedToLayout(options: List<VoteOption>, index: Int) =
        options[index] == options[options.size - 1]

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