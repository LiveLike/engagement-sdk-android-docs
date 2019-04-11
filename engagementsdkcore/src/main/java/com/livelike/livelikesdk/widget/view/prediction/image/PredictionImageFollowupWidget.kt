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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.util.AndroidResource.Companion.dpToPx
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil.Companion.correctAnswerLottieFilePath
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil.Companion.wrongAnswerLottieFilePath
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.cross_image.view.*
import kotlinx.android.synthetic.main.prediction_image_row_element.view.*
import kotlinx.android.synthetic.main.prediction_image_widget.view.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class PredictionImageFollowupWidget : ConstraintLayout, WidgetObserver {
    private var dismissWidget: (() -> Unit)? = null
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimationManager
    lateinit var widgetResultDisplayUtil: WidgetResultDisplayUtil
    private var layout = ConstraintLayout(context, null, 0)
    private var timeout = 0L
    private var initialTimeout = 0L
    private var parentWidth = 0
    private lateinit var state: (WidgetTransientState) -> Unit
    private lateinit var animationProperties: WidgetTransientState
    private var transientState = WidgetTransientState()
    // TODO: Duplicate of text follow up. Move out.
    private var executor = ScheduledThreadPoolExecutor(15)
    lateinit var future: ScheduledFuture<*>
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun initialize(dismiss: () -> Unit, properties: WidgetTransientState, parentWidth: Int, viewAnimation: ViewAnimationManager, state: (WidgetTransientState) -> Unit) {
        dismissWidget = dismiss
        this.timeout = properties.timeout
        this.parentWidth = parentWidth
        this.viewAnimation = viewAnimation
        this.state = state
        this.animationProperties = properties
        future = executor.scheduleAtFixedRate(Updater(), 0, 1, TimeUnit.SECONDS)
        inflate(context)
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
        widgetResultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
        prediction_question_textView.layoutParams.width = parentWidth
    }

    private fun updateCrossImage() {
        prediction_followup_image_cross.apply {
            setImageResource(R.mipmap.widget_ic_x)
            setOnClickListener { dismissWidget?.invoke() }
        }
    }

    inner class Updater: Runnable {
        override fun run() {
            transientState.timeout = timeout - initialTimeout
            state.invoke(transientState)
            val updateRate = 1000
            initialTimeout += updateRate
            if (timeout == initialTimeout) {
                future.cancel(false)
            }
        }
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
        viewAnimation.startWidgetTransitionInAnimation {
            widgetResultDisplayUtil.startResultAnimation(correctOption == userSelectedOption, prediction_result,
                {
                    transientState.resultAnimatorStartPhase = it
                    state.invoke(transientState)
                },
                {
                    transientState.resultAnimationPath = it
                    state.invoke(transientState)
                }, animationProperties)
        }
        initAdapter(voteOptions, correctOption, userSelectedOption)
        lottieAnimationPath = findResultAnimationPath(correctOption, userSelectedOption)
        transitionAnimation()
    }

    private fun findResultAnimationPath(correctOption: String?, userSelectedOption: String?): String {
        return if (hasUserSelectedCorrectOption(correctOption, userSelectedOption))
            correctAnswerLottieFilePath
        else wrongAnswerLottieFilePath
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout)
    }

    private fun initAdapter(voteOptions: List<VoteOption>, correctOption: String?, userSelectedOption: String?) {
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        image_optionList.layoutManager = linearLayoutManager
        image_optionList.adapter = ImageAdapter(voteOptions, correctOption, userSelectedOption)
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {}
    override fun confirmMessageUpdated(confirmMessage: String) {}

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
            widgetResultDisplayUtil.updateViewDrawable(option.id,
                progressBar,
                holder.button,
                option.votePercentage.toInt(),
                correctOption,
                userSelectedOption)
            widgetResultDisplayUtil.setImageViewMargin(option, optionList, holder.itemView)
        }

        private fun loadImage(option: VoteOption, imageWidth: Int, optionButton: ImageView) {
            Glide.with(context)
                .load(option.imageUrl)
                .apply(
                    RequestOptions()
                        .override(imageWidth, imageWidth)
                        .transform(MultiTransformation(FitCenter(), RoundedCorners(12)))
                )
                .into(optionButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(
                R.layout.prediction_image_row_element,
                parent,
                false
            )
            widgetResultDisplayUtil.setImageItemWidth(optionList, view, parentWidth)
            return ViewHolder(
                view
            )
        }

        override fun getItemCount(): Int {
            return optionList.size
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: View = view.button
        val optionButton: ImageView = view.image_button
        val optionText: TextView = view.item_text
        val percentageText: TextView = view.result_percentage_text
        val progressBar: ProgressBar = view.determinateBar
    }
}