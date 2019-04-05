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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.binding.QuizVoteObserver
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.prediction_image_row_element.view.*
import kotlinx.android.synthetic.main.prediction_image_widget.view.*

class QuizImageWidget : ConstraintLayout, WidgetObserver, QuizVoteObserver {
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimationManager
    private lateinit var resultDisplayUtil: WidgetResultDisplayUtil
    private lateinit var userTapped: () -> Unit
    private val imageButtonMap = HashMap<View, String?>()
    private val viewOptions = HashMap<String?, ViewOption>()
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    private var fetchResult: (() -> Unit)? = null
    private var selectedOption : String? = null
    private var correctOption: String? = null
    private var timeout = 0L
    private var showResults = false
    var parentWidth = 0
    
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_image_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_image_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        // TODO: Maybe inject this object.
        viewAnimation.startWidgetTransitionInAnimation {
            viewAnimation.startTimerAnimation(pieTimer, timeout, {
                fetchResult?.invoke()
                showResults = true
            }, { })
        }
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        image_optionList.layoutManager = linearLayoutManager

        resultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
        prediction_question_textView.layoutParams.width = parentWidth
    }

    internal fun initialize(
        dismiss: () -> Unit,
        timeout: Long,
        fetch: () -> Unit,
        parentWidth: Int,
        viewAnimation: ViewAnimationManager
    ) {
        this.timeout = timeout
        dismissWidget = dismiss
        fetchResult = fetch
        this.viewAnimation = viewAnimation
        this.parentWidth = parentWidth
        inflate(context)
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
        image_optionList.adapter?.let {
            if (showResults)
                updateVoteCount(voteOptions)
        } ?: run { image_optionList.adapter = ImageAdapter(voteOptions, optionSelectedCallback) }
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        selectedOption = selectedOptionId
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
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout)

        resultDisplayUtil.startResultAnimation(correctOption == selectedOption, prediction_result, {}, {})
        voteOptions.forEach { option ->
            val viewOption = viewOptions[option.id]
            if (viewOption != null) {
                viewOption.progressBar.progress = option.answerCount.toInt()
                viewOption.percentageTextView.text = option.answerCount.toString().plus("%")
                resultDisplayUtil.updateViewDrawable(
                    option.id,
                    viewOption.progressBar,
                    viewOption.button,
                    option.answerCount.toInt(),
                    correctOption,
                    selectedOption
                )
            }
            viewOption?.button?.setOnClickListener(null)
        }
    }


    inner class ImageAdapter(
        private val optionList: List<VoteOption>,
        private val optionSelectedCallback: (String?) -> Unit
    ) : RecyclerView.Adapter<ViewHolder>() {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = optionList[position]
            holder.optionText.text = option.description
            option.isCorrect.let { if(it) correctOption = option.id }

            // TODO: Move this to adapter layer.
            Glide.with(context)
                .load(option.imageUrl)
                .apply(
                    RequestOptions().override(AndroidResource.dpToPx(74), AndroidResource.dpToPx(74)).transform(
                        MultiTransformation(FitCenter(), RoundedCorners(12))
                    )
                )
                .into(holder.optionButton)
            imageButtonMap[holder.button] = option.id
            holder.button.setOnClickListener {
                selectedOption = imageButtonMap[holder.button].toString()
                optionSelectedCallback(selectedOption)
                userTapped.invoke()
            }
            viewOptions[option.id] = ViewOption(
                holder.button,
                holder.progressBar,
                holder.percentageText
            )
            resultDisplayUtil.setImageViewMargin(option, optionList, holder.itemView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(
                R.layout.prediction_image_row_element,
                parent,
                false
            )

            resultDisplayUtil.setImageItemWidth(optionList, view, parentWidth)
            return ViewHolder(
                view
            )
        }

        override fun getItemCount(): Int {
            return optionList.size
        }
    }

    class ViewOption(
        val button: View,
        val progressBar: ProgressBar,
        val percentageTextView: TextView
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: View = view.button
        val optionButton: ImageView = view.image_button
        val optionText: TextView = view.item_text
        val percentageText: TextView = view.result_percentage_text
        val progressBar: ProgressBar = view.determinateBar
    }
}