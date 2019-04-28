package com.livelike.livelikesdk.widget.view.prediction.text

import android.annotation.SuppressLint
import android.content.Context
import android.support.constraint.ConstraintLayout
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
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.util.WidgetTimeoutUpdater
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil.Companion.correctAnswerLottieFilePath
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil.Companion.wrongAnswerLottieFilePath
import kotlinx.android.synthetic.main.confirm_message.view.confirmMessageTextView
import kotlinx.android.synthetic.main.confirm_message.view.prediction_result
import kotlinx.android.synthetic.main.prediction_text_widget.view.option_list
import kotlinx.android.synthetic.main.prediction_text_widget.view.questionTextView
import kotlinx.android.synthetic.main.text_option_row_element.view.determinateBar
import kotlinx.android.synthetic.main.text_option_row_element.view.percentageText
import kotlinx.android.synthetic.main.text_option_row_element.view.text_button
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

open class TextOptionWidgetBase : ConstraintLayout, WidgetObserver {
    private lateinit var userTapped: () -> Unit
    private lateinit var viewAnimation: ViewAnimationManager
    private lateinit var resultDisplayUtil: WidgetResultDisplayUtil
    private var optionAdapter: TextOptionAdapter? = null
    private var lottieAnimationPath = ""
    protected lateinit var pieTimerViewStub: ViewStub
    protected lateinit var progressedStateCallback: (WidgetTransientState) -> Unit
    protected lateinit var startingState: WidgetTransientState
    protected lateinit var progressedState: WidgetTransientState
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
    var interactionPhaseTimeout = 0L
    var resultPhaseTimeout = 0L
    var initialTimeout = 0L
    val updateRate = 1000
    var executor = ScheduledThreadPoolExecutor(15)
    lateinit var future: ScheduledFuture<*>

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    internal open fun initialize(
        dismiss: () -> Unit,
        startingState: WidgetTransientState,
        progressedState: WidgetTransientState,
        parentWidth: Int,
        viewAnimation: ViewAnimationManager,
        progressedStateCallback: (WidgetTransientState) -> Unit
    ) {
        dismissWidget = dismiss
        this.viewAnimation = viewAnimation
        this.startingState = startingState
        this.progressedState = progressedState
        this.progressedStateCallback = progressedStateCallback
        this.interactionPhaseTimeout = startingState.interactionPhaseTimeout
        this.resultPhaseTimeout = startingState.resultPhaseTimeout
        inflate(context)
        questionTextView.layoutParams.width = parentWidth
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_text_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_text_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
        viewAnimation.addHorizontalSwipeListener(questionTextView, layout, dismissWidget)
        resultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
        future = executor.scheduleAtFixedRate(Updater(), 0, 1000, TimeUnit.MILLISECONDS)
    }

    fun userTappedCallback(userTapped: () -> Unit) {
        this.userTapped = userTapped
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun questionUpdated(questionText: String) {
        questionTextView.text = questionText
    }

    override fun confirmMessageUpdated(confirmMessage: String) {
        confirmMessageTextView.text = confirmMessage
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>
    ) {
        optionAdapter?.updateOptionList(voteOptions, correctOptionWithUserSelection) ?: run {
            optionAdapter = TextOptionAdapter(voteOptions, optionSelectedCallback, correctOptionWithUserSelection)
            option_list.adapter = optionAdapter
            option_list.apply {
                adapter = optionAdapter
                optionAdapter?.updateOptionList(voteOptions, correctOptionWithUserSelection)
            }
        }
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        progressedState.userSelection = selectedOptionId
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
        viewAnimation.startResultAnimation(
            lottieAnimationPath, context, prediction_result,
            {
                progressedState.resultAnimatorStartPhase = it
                progressedStateCallback.invoke(progressedState)
            },
            {
                progressedState.resultAnimationPath = it
                progressedStateCallback.invoke(progressedState)
            }, startingState
        )
    }

    inner class Updater: Runnable {
        private var timeoutUpdater : WidgetTimeoutUpdater = WidgetTimeoutUpdater(updateRate, progressedState, progressedStateCallback)

        init {
            timeoutUpdater.initialTimeout = initialTimeout
        }

        override fun run() {
            when {
                showResults -> {
                    timeoutUpdater.updateResultPhaseTimeout(
                        interactionPhaseTimeout,
                        resultPhaseTimeout
                    ) { future.cancel(false) }
                }
                else -> {
                    timeoutUpdater.updateInteractionPhaseTimeout(
                        interactionPhaseTimeout,
                        resultPhaseTimeout)
                    {
                        timeoutUpdater.initialTimeout = 0L
                        showResults = true
                    }
                }
            }
        }
    }

    inner class TextOptionAdapter(
        private var optionList: List<VoteOption>,
        private val optionSelectedCallback: (String?) -> Unit,
        private var correctOptionWithUserSelection: Pair<String?, String?>
    ) : RecyclerView.Adapter<ViewHolder>() {

        fun updateOptionList(data: List<VoteOption>, correctOptionWithUserSelection: Pair<String?, String?>) {
            this.correctOptionWithUserSelection = correctOptionWithUserSelection
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
            if (option == optionList[optionList.size - 1] && progressedState.userSelection != null)
                optionSelectedUpdated(progressedState.userSelection)

            if (showResults) {
                setResultsBackground(holder, option.id, option.votePercentage.toInt())
            }

            if (buttonClickEnabled) {
                button.setOnClickListener {
                    prevOptionSelectedId = optionSelectedId
                    optionSelectedId = option.id
                    optionSelectedCallback(option.id)
                    userTapped.invoke()
                }
            } else {
                button.setOnClickListener(null)
            }

            viewAnimation.addHorizontalSwipeListener(button, layout, dismissWidget)
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
                correctOptionWithUserSelection.first,
                correctOptionWithUserSelection.second,
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

        var option: VoteOption? = null
            set(voteOption) {
                field = voteOption
                optionButton.text = voteOption?.description
            }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        future.cancel(false)
    }
}