package com.livelike.livelikesdk.widget.view.prediction.quiz

import android.annotation.SuppressLint
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.binding.QuizWidgetObserver
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextFollowUpWidgetView
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.prediction_image_row_element.view.*
import kotlinx.android.synthetic.main.prediction_image_widget.view.*

class QuizImageWidget : ConstraintLayout, WidgetObserver, QuizWidgetObserver {
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimation
    private var optionSelected = false
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    private var fetchResult: ((List<String>) -> Unit)? = null
    val imageButtonMap = HashMap<ImageButton, String?>()
    var selectedOption: String? = null
    var correctOption: String? = null
    lateinit var userTapped: () -> Unit
    private val answerUrlList = arrayListOf<String>()
    private val viewOptions = ArrayList<ViewOption>()
    private var lottieAnimationPath = ""

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, dismiss: () -> Unit) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        dismissWidget = dismiss
    }

    init { inflate(context) }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_image_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_image_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        // TODO: Maybe inject this object.
        viewAnimation = ViewAnimation(this)
        viewAnimation.startWidgetTransitionInAnimation {
            viewAnimation.startTimerAnimation(pieTimer, 7000) {
                if (optionSelected)
                    fetchResult?.invoke(answerUrlList)

                dismissWidget?.invoke()
            }
        }
    }

    override fun questionUpdated(questionText: String) {
        viewAnimation.addHorizontalSwipeListener(prediction_question_textView.apply {
            text = questionText
            isClickable = true
        }, layout, dismissWidget)
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>
    ) {
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        image_optionList.layoutManager = linearLayoutManager
        image_optionList.adapter = ImageAdapter(voteOptions, optionSelectedCallback)
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        optionSelected = true
        imageButtonMap.forEach { (button, id) ->
            if (selectedOptionId == id)
                button.background = AppCompatResources.getDrawable(context, R.drawable.button_pressed)
            else button.background = AppCompatResources.getDrawable(context, R.drawable.button_rounded_corners)
        }
    }

    override fun confirmMessageUpdated(confirmMessage: String) {
        prediction_confirm_message_textView.text = confirmMessage
    }

    fun userTappedCallback(userTapped: () -> Unit) {
        this.userTapped = userTapped
    }

    // TODO: Remove double iteration and use map instead.
    override fun updateVoteCount(voteOptions: List<VoteOption>) {
        viewOptions.forEach { view ->
            voteOptions.forEach { option ->
                if (view.id == option.id) {
                    view.progressBar.progress = option.answerCount.toInt()
                    view.percentageTextView.text = option.answerCount.toString().plus("%")
                    updateViewDrawable(option, view.progressBar, view.button, option.answerCount.toInt())
                }

            }
        }
    }
    private fun hasUserSelectedCorrectOption(userSelectedOption: String?, correctOption: String?) =
        userSelectedOption == correctOption

    private fun findResultAnimationPath(correctOption: String?, userSelectedOption: String?): String {
        return if (hasUserSelectedCorrectOption(correctOption, userSelectedOption))
            PredictionTextFollowUpWidgetView.correctAnswerLottieFilePath
        else PredictionTextFollowUpWidgetView.wrongAnswerLottieFilePath
    }
    private fun updateViewDrawable(option: VoteOption, progressBar: ProgressBar, optionButton: ImageButton, percentage: Int) {
        lottieAnimationPath = findResultAnimationPath(correctOption, selectedOption)
        if (hasUserSelectedCorrectOption(selectedOption, correctOption)) {
            if (isCurrentButtonSameAsCorrectOption(correctOption, option.id)) {
                updateProgressBar(progressBar, R.drawable.progress_bar_user_correct, percentage)
                updateImageButton(optionButton, R.drawable.button_correct_answer_outline)
            } else {
                updateProgressBar(progressBar, R.drawable.progress_bar_wrong_option, percentage)
            }
        } else {
            when {
                isCurrentButtonSameAsCorrectOption(correctOption, option.id) -> {
                    updateProgressBar(progressBar, R.drawable.progress_bar_user_correct, percentage)
                    updateImageButton(optionButton, R.drawable.button_correct_answer_outline)
                }
                isCurrentButtonSameAsCorrectOption(selectedOption, option.id) -> {
                    updateProgressBar(progressBar, R.drawable.progress_bar_user_selection_wrong, percentage)
                    updateImageButton(optionButton, R.drawable.button_wrong_answer_outline)
                }
                else -> {
                    updateProgressBar(progressBar, R.drawable.progress_bar_wrong_option, percentage)
                }
            }
        }
    }

    private fun updateProgressBar(progressBar: ProgressBar, drawable: Int, percentage: Int) {
        progressBar.apply {
            progressDrawable = AppCompatResources.getDrawable(context, drawable)
            visibility = View.VISIBLE
            progress = percentage
        }
    }

    private fun updateImageButton(button: ImageButton, drawable: Int) {
        button.background = AppCompatResources.getDrawable(context, drawable)
        setOnClickListener(null)
    }

    private fun overrideButtonPadding(optionButton: ImageButton) {
        optionButton.setPadding(
            AndroidResource.dpToPx(2),
            AndroidResource.dpToPx(14),
            AndroidResource.dpToPx(48),
            AndroidResource.dpToPx(2)
        )
    }

    private fun isCurrentButtonSameAsCorrectOption(correctOption: String?, buttonId: String?) =
        buttonId == correctOption

    inner class ImageAdapter(
        private val optionList: List<VoteOption>,
        private val optionSelectedCallback: (String?) -> Unit

    ) : RecyclerView.Adapter<ViewHolder>() {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = optionList[position]
            holder.optionText.text = option.description
            option.answerUrl?.let { answerUrlList.add(it) }
            option.isCorrect?.let { if(it) correctOption = option.id }

            // TODO: Move this to adapter layer.
            Glide.with(context)
                .load(option.imageUrl)
                .into(holder.optionButton)
            imageButtonMap[holder.optionButton] = option.id
            holder.optionButton.setOnClickListener {
                selectedOption = imageButtonMap[holder.optionButton]
                optionSelectedCallback(selectedOption)
                userTapped.invoke()
            }
            viewOptions.add(ViewOption(holder.optionButton, option.id, holder.progressBar, holder.percentageText))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(context).inflate(
                    R.layout.prediction_image_row_element,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return optionList.size
        }
    }
    class ViewOption(
        val button: ImageButton,
        val id: String?,
        val progressBar: ProgressBar,
        val percentageTextView: TextView
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val optionButton: ImageButton = view.image_button
        val optionText: TextView = view.item_text
        val percentageText: TextView = view.result_percentage_text
        val progressBar: ProgressBar = view.determinateBar
    }
}