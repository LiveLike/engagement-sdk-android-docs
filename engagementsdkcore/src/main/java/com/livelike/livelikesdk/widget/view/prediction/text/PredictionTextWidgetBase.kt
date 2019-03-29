package com.livelike.livelikesdk.widget.view.prediction.text

import android.annotation.SuppressLint
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.model.VoteOption
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.prediction_text_widget.view.*
import kotlinx.android.synthetic.main.text_option_row_element.view.*

open class PredictionTextWidgetBase : ConstraintLayout, WidgetObserver {
    protected val widgetOpacityFactor: Float = 0.2f
    protected val constraintSet = ConstraintSet()
    protected val buttonList: ArrayList<Button> = ArrayList()
    protected val buttonMap = mutableMapOf<Button, String?>()
    protected val viewMap = mutableMapOf<ViewHolder, String?>()
    protected var optionSelected = false
    protected var layout = ConstraintLayout(context, null, 0)
    protected var lottieAnimationPath = ""
    protected lateinit var pieTimerViewStub: ViewStub
    protected var dismissWidget :  (() -> Unit)? = null
    protected var showResults = false;
    private lateinit var userTapped : () -> Unit
    private lateinit var viewAnimation: ViewAnimation

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    open fun initialize(dismiss: ()->Unit, timeout: Long) {
        inflate(context)
        dismissWidget = dismiss
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
                .inflate(R.layout.prediction_text_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_text_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
        viewAnimation = ViewAnimation(this)
    }

    fun userTappedCallback(userTapped: () -> Unit) {
        this.userTapped = userTapped
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun questionUpdated(questionText: String) {
        viewAnimation.addHorizontalSwipeListener(prediction_question_textView.apply {
            text = questionText
            isClickable = true
        }, layout, dismissWidget)
    }

    override fun confirmMessageUpdated(confirmMessage: String) {
        prediction_confirm_message_textView.text = confirmMessage
    }

    override fun optionListUpdated(voteOptions: List<VoteOption>, optionSelectedCallback: (String?) -> Unit, correctOptionWithUserSelection: Pair<String?, String?>) {
        option_list.adapter = TextOptionAdapter(voteOptions, optionSelectedCallback, correctOptionWithUserSelection)
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        buttonMap.forEach { (button, id) ->
            if (selectedOptionId == id)
                button.background = AppCompatResources.getDrawable(context, R.drawable.button_pressed)
            else button.background = AppCompatResources.getDrawable(context, R.drawable.button_default)
        }
    }

    protected fun dismissWidget() {
        dismissWidget?.invoke()
    }

    inner class TextOptionAdapter(
        private val optionList: List<VoteOption>,
        private val optionSelectedCallback: (String?) -> Unit,
        private val correctOptionWithUserSelection: Pair<String?, String?>) : RecyclerView.Adapter<ViewHolder>() {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = optionList[position]
            val button = holder.optionButton

            button.text = option.description
            buttonMap[button] = option.id
            viewMap[holder] = option.id

            if (showResults) {
                setResultsBackground(holder, option.id, option.votePercentage.toInt())
                button.setOnClickListener(null)
            } else {
                button.setOnClickListener {
                    optionSelected = true
                    val selectedButton = buttonMap[button]
                    optionSelectedCallback(selectedButton)
                    userTapped.invoke()
                }
            }

            if(position == optionList.lastIndex)
                button.background = AppCompatResources.getDrawable(context, R.drawable.bottom_rounded_corner)

            viewAnimation.addHorizontalSwipeListener(button, layout, dismissWidget)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(context).inflate(R.layout.text_option_row_element, parent, false)
            )
        }

        private fun setResultsBackground(viewHolder: ViewHolder, optionId: String?, votePercentage: Int) {
            val correctOption = correctOptionWithUserSelection.first
            val userSelectedOption = correctOptionWithUserSelection.second
            val optionCorrect = optionId == correctOption

            viewHolder.optionButton.setOnClickListener(null)

            viewHolder.optionButton.background = if (optionCorrect)
                AppCompatResources.getDrawable(context, R.drawable.button_correct_answer_outline)
            else
                AppCompatResources.getDrawable(context, R.drawable.button_wrong_answer_outline)

            if (optionCorrect)
                viewHolder.progressBar.progressDrawable =
                        AppCompatResources.getDrawable(context, R.drawable.progress_bar_user_correct)
            else
                viewHolder.progressBar.progressDrawable = if (optionId == userSelectedOption)
                    AppCompatResources.getDrawable(context, R.drawable.progress_bar_user_selection_wrong)
                        else AppCompatResources.getDrawable(context, R.drawable.progress_bar_wrong_option)

            viewHolder.percentText.text = votePercentage.toString().plus("%")
            viewHolder.progressBar.apply {
                visibility = View.VISIBLE
                progress = votePercentage
            }
        }

        override fun getItemCount(): Int {
            return optionList.size
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val optionButton: Button = view.text_button
        val percentText: TextView = view.result_percentage_text
        val progressBar: ProgressBar = view.determinateBar
    }
}