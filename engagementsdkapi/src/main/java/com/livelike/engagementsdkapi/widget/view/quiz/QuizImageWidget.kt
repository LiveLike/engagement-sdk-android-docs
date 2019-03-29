package com.livelike.livelikesdk.widget.view.quiz

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
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
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.prediction_image_row_element.view.*
import kotlinx.android.synthetic.main.prediction_image_widget.view.*

class QuizImageWidget : ConstraintLayout, WidgetObserver, QuizWidgetObserver {
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimation
    private lateinit var resultDisplayUtil: WidgetResultDisplayUtil
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    private var fetchResult: (() -> Unit)? = null
    val imageButtonMap = HashMap<ImageButton, String?>()
    var selectedOption: String? = null
    var correctOption: String? = null
    lateinit var userTapped: () -> Unit
    private val viewOptions = HashMap<String?, ViewOption>()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, dismiss: () -> Unit, fetch: () -> Unit) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        dismissWidget = dismiss
        fetchResult = fetch
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
                if (selectedOption == null)
                    dismissWidget?.invoke()
                else fetchResult?.invoke()
            }
        }
        resultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
    }

    override fun questionUpdated(questionText: String) {
        viewAnimation.addHorizontalSwipeListener(prediction_question_textView.apply {
            text = questionText
            isClickable = true
            background = AppCompatResources.getDrawable(context, R.drawable.quiz_textview_rounded_corner)
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
        imageButtonMap.forEach { (button, id) ->
            if (selectedOptionId == id)
                button.background = AppCompatResources.getDrawable(context, R.drawable.quiz_button_pressed)
            else button.background = AppCompatResources.getDrawable(context, R.drawable.button_rounded_corners)
        }
    }

    override fun confirmMessageUpdated(confirmMessage: String) {
        prediction_confirm_message_textView.text = confirmMessage
    }

    fun userTappedCallback(userTapped: () -> Unit) {
        this.userTapped = userTapped
    }

    override fun updateVoteCount(voteOptions: List<VoteOption>) {
        Handler().postDelayed({ viewAnimation.triggerTransitionOutAnimation { dismissWidget?.invoke() } }, 6000)
        voteOptions.forEach { option ->
            val viewOption = viewOptions[option.id]
            if (viewOption != null) {
                viewOption.progressBar.progress = option.answerCount.toInt()
                viewOption.percentageTextView.text = option.answerCount.toString().plus("%")
                resultDisplayUtil.updateViewDrawable(option,
                    viewOption.progressBar,
                    viewOption.button,
                    option.answerCount.toInt(),
                    correctOption,
                    selectedOption,
                    prediction_result)
            }
        }
    }

    inner class ImageAdapter(
        private val optionList: List<VoteOption>,
        private val optionSelectedCallback: (String?) -> Unit
    ) : RecyclerView.Adapter<ViewHolder>() {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = optionList[position]
            holder.optionText.text = option.description
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
            viewOptions[option.id] = ViewOption(
                holder.optionButton,
                holder.progressBar,
                holder.percentageText
            )
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