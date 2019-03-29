package com.livelike.livelikesdk.widget.view.prediction.image

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.support.constraint.ConstraintLayout
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
import com.bumptech.glide.request.RequestOptions
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.util.AndroidResource.Companion.dpToPx
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.WidgetResultDisplayUtil
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextFollowUpWidgetView
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.cross_image.view.*
import kotlinx.android.synthetic.main.prediction_image_row_element.view.*
import kotlinx.android.synthetic.main.prediction_image_widget.view.*

internal class PredictionImageFollowupWidget : ConstraintLayout, WidgetObserver {
    private var dismissWidget: (() -> Unit)? = null
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimation
    lateinit var widgetResultDisplayUtil: WidgetResultDisplayUtil
    private var layout = ConstraintLayout(context, null, 0)
    private var lottieAnimationPath = ""

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var timeout = 0L
    fun initialize(dismiss: () -> Unit, timeout: Long) {
        inflate(context)
        dismissWidget = dismiss
        this.timeout = timeout
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_image_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_image_widget)

        pieTimerViewStub = findViewById(R.id.prediction_pie)
        pieTimerViewStub.layoutResource = R.layout.cross_image
        pieTimerViewStub.inflate()

        updateCrossImage()
        viewAnimation = ViewAnimation(this)
        widgetResultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
    }

    private fun updateCrossImage() {
        prediction_followup_image_cross.apply {
            setImageResource(R.mipmap.widget_ic_x)
            setOnClickListener { dismissWidget?.invoke() }
        }
    }

    private fun transitionAnimation() {
        viewAnimation.startWidgetTransitionInAnimation{
            viewAnimation.startResultAnimation(lottieAnimationPath, context, prediction_result)
        }
        Handler().postDelayed(
            { viewAnimation.triggerTransitionOutAnimation { dismissWidget?.invoke() } },
            timeout
        )
    }

    override fun questionUpdated(questionText: String) {
        viewAnimation.addHorizontalSwipeListener(prediction_question_textView.apply {
            text = questionText
            isClickable = true
        }, layout, dismissWidget)
    }

    override fun optionListUpdated(voteOptions: List<VoteOption>,
                                   optionSelectedCallback: (String?) -> Unit,
                                   correctOptionWithUserSelection: Pair<String?, String?>) {
        val correctOption = correctOptionWithUserSelection.first
        val userSelectedOption = correctOptionWithUserSelection.second
        initAdapter(voteOptions, correctOption, userSelectedOption)
        lottieAnimationPath = findResultAnimationPath(correctOption, userSelectedOption)
        transitionAnimation()
    }

    private fun findResultAnimationPath(correctOption: String?, userSelectedOption: String?): String {
        return if (hasUserSelectedCorrectOption(correctOption, userSelectedOption))
            PredictionTextFollowUpWidgetView.correctAnswerLottieFilePath
        else PredictionTextFollowUpWidgetView.wrongAnswerLottieFilePath
    }

    private fun initAdapter(voteOptions: List<VoteOption>, correctOption: String?, userSelectedOption: String?) {
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        image_optionList.layoutManager = linearLayoutManager
        image_optionList.adapter = ImageAdapter(voteOptions, correctOption, userSelectedOption)
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {}
    override fun confirmMessageUpdated(confirmMessage: String) {}

    fun hasUserSelectedCorrectOption(userSelectedOption: String?, correctOption: String?) =
        userSelectedOption == correctOption

    inner class ImageAdapter(
        private val optionList: List<VoteOption>,
        private val correctOption: String?,
        private val userSelectedOption: String?
    ) : RecyclerView.Adapter<ViewHolder>() {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = optionList[position]
            val progressBar = holder.progressBar
            val optionButton = holder.optionButton

            holder.optionText.text = option.description
            widgetResultDisplayUtil.updatePercentageText(holder.percentageText, option)
            loadImage(option, dpToPx(74), optionButton)
            widgetResultDisplayUtil.updateViewDrawable(option,
                progressBar,
                optionButton,
                option.votePercentage.toInt(),
                correctOption,
                userSelectedOption,
                prediction_result)
            overrideButtonPadding(optionButton)
        }

        private fun loadImage(option: VoteOption, imageWidth: Int, optionButton: ImageButton) {
            Glide.with(context)
                .load(option.imageUrl)
                .apply(RequestOptions().override(imageWidth, imageWidth))
                .into(optionButton)
        }

        private fun overrideButtonPadding(optionButton: ImageButton) {
            optionButton.setPadding(dpToPx(2), dpToPx(14), dpToPx(48), dpToPx(2))
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val optionButton: ImageButton = view.image_button
        val optionText: TextView = view.item_text
        val percentageText: TextView = view.result_percentage_text
        val progressBar: ProgressBar = view.determinateBar
    }
}