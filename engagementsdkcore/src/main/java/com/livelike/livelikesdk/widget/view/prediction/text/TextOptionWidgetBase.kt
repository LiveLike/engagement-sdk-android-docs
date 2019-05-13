package com.livelike.livelikesdk.widget.view.prediction.text

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
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
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.widget.model.Option
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil.Companion.correctAnswerLottieFilePath
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil.Companion.wrongAnswerLottieFilePath
import kotlinx.android.synthetic.main.confirm_message.view.confirmMessageTextView
import kotlinx.android.synthetic.main.prediction_text_widget.view.option_list
import kotlinx.android.synthetic.main.prediction_text_widget.view.questionTextView
import kotlinx.android.synthetic.main.text_option_row_element.view.determinateBar
import kotlinx.android.synthetic.main.text_option_row_element.view.percentageText
import kotlinx.android.synthetic.main.text_option_row_element.view.text_button

internal class TextOptionWidgetBase : ConstraintLayout, WidgetObserver {
    override fun questionUpdated(questionText: String) {
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>
    ) {
    }

    override fun confirmMessageUpdated(confirmMessage: String) {
    }


    //    private lateinit var userTapped: () -> Unit
    private var viewAnimation = ViewAnimationManager(this)
    private var resultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
    private var optionAdapter: TextOptionAdapter? = null
    private var lottieAnimationPath = ""

    protected lateinit var pieTimerViewStub: ViewStub
    protected val widgetOpacityFactor: Float = 0.2f
    protected val buttonList: ArrayList<Button> = ArrayList()
    protected val buttonMap = mutableMapOf<Button, String?>()
    protected var optionSelectedId = ""
    protected var prevOptionSelectedId = ""
    protected var layout = ConstraintLayout(context, null, 0)
    protected var dismissWidget: (() -> Unit)? = null
    protected var showResults = false
    protected var buttonClickEnabled = true
    protected var useNeutralValues = false

    private var defaultButtonDrawable =
        AppCompatResources.getDrawable(context, com.livelike.livelikesdk.R.drawable.button_default)
    protected var selectedButtonDrawable =
        AppCompatResources.getDrawable(context, com.livelike.livelikesdk.R.drawable.prediction_button_pressed)

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var textOptionWidgetViewModel =
        ViewModelProviders.of(context as AppCompatActivity).get(TextOptionWidgetViewModel::class.java)

    init {
        dismissWidget = { textOptionWidgetViewModel.dismiss() }
        inflate(context)

        // could use LiveData for live update
        questionTextView.text = textOptionWidgetViewModel.data.question
        confirmMessageTextView.text = textOptionWidgetViewModel.data.confirmation_message

        textOptionWidgetViewModel.optionAdapter?.updateOptionList(textOptionWidgetViewModel.data.options) ?: run {
            optionAdapter = TextOptionAdapter(textOptionWidgetViewModel.data.options) { optionSelectedCallback(it) }
            option_list.adapter = optionAdapter
            option_list.apply {
                adapter = optionAdapter
                optionAdapter?.updateOptionList(textOptionWidgetViewModel.data.options)
            }
        }
    }

    fun optionSelectedCallback(selectedOptionId: String?) {
        textOptionWidgetViewModel.userSelection = selectedOptionId
        optionSelectedId = selectedOptionId.toString()
        buttonMap.forEach { (button, id) ->
            if (selectedOptionId == id)
                button.background = selectedButtonDrawable
            else button.background = defaultButtonDrawable
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_text_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_text_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
    }

//    fun userTappedCallback(userTapped: () -> Unit) {
//        this.userTapped = userTapped
//    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        textOptionWidgetViewModel.userSelection = selectedOptionId
        optionSelectedId = selectedOptionId.toString()
        buttonMap.forEach { (button, id) ->
            if (selectedOptionId == id)
                button.background = selectedButtonDrawable
            else button.background = defaultButtonDrawable
        }
    }

    protected fun dismissWidget() {
        dismissWidget?.invoke()
    }

    protected fun showResultsAnimation(correctOptionWithUserSelection: Pair<String?, String?>) {
        lottieAnimationPath = if (correctOptionWithUserSelection.first == correctOptionWithUserSelection.second)
            correctAnswerLottieFilePath
        else wrongAnswerLottieFilePath
//        viewAnimation.startResultAnimation(
//            lottieAnimationPath, context, prediction_result
//        )
    }

    inner class TextOptionAdapter(
        private var optionList: List<Option>,
        private val optionSelectedCallback: (String?) -> Unit
    ) : RecyclerView.Adapter<ViewHolder>() {

        fun updateOptionList(data: List<Option>) {
            optionList = data
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = optionList[position]
            val button = holder.optionButton

            holder.option = option
            buttonMap[button] = option.id
            // This is needed here as notifyDataSetChanged() is behaving asynchronously. So after device config change need
            // a way to update user selection.
            if (option == optionList[optionList.size - 1] && textOptionWidgetViewModel.userSelection != null)
                optionSelectedUpdated(textOptionWidgetViewModel.userSelection)

            if (showResults) {
                setResultsBackground(holder, option.id, option.getPercentVote(option.vote_count.toFloat()).toInt())
            }

            if (buttonClickEnabled) {
                button.setOnClickListener {
                    prevOptionSelectedId = optionSelectedId
                    optionSelectedId = option.id
                    optionSelectedCallback(option.id)
                }
            } else {
                button.setOnClickListener(null)
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(context).inflate(R.layout.text_option_row_element, parent, false)
            val lp = RecyclerView.LayoutParams(
                (parent as RecyclerView).layoutManager!!.width,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            itemView.layoutParams = lp
            return ViewHolder(itemView)
        }

        private fun setResultsBackground(viewHolder: ViewHolder, optionId: String, votePercentage: Int) {
            viewHolder.percentText.text = votePercentage.toString().plus("%")
            viewHolder.progressBar.apply {
                visibility = View.VISIBLE
                progress = votePercentage
            }
            resultDisplayUtil.updateViewDrawable(
                optionId,
                viewHolder.progressBar,
                viewHolder.optionButton,
                votePercentage,
                textOptionWidgetViewModel.data.correct_option_id,
                textOptionWidgetViewModel.userSelection,
                useNeutralValues
            )
        }

        override fun getItemCount(): Int {
            return optionList.size
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val optionButton: Button = view.text_button
        val percentText: TextView = view.percentageText
        val progressBar: ProgressBar = view.determinateBar

        var option: Option? = null
            set(voteOption) {
                field = voteOption
                optionButton.text = voteOption?.description
            }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}