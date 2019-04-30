package com.livelike.livelikesdk.widget.view.poll

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
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
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.binding.QuizVoteObserver
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil
import kotlinx.android.synthetic.main.confirm_message.view.confirmMessageTextView
import kotlinx.android.synthetic.main.prediction_image_row_element.view.button
import kotlinx.android.synthetic.main.prediction_image_row_element.view.determinateBar
import kotlinx.android.synthetic.main.prediction_image_row_element.view.image_button
import kotlinx.android.synthetic.main.prediction_image_row_element.view.item_text
import kotlinx.android.synthetic.main.prediction_image_row_element.view.percentageText
import kotlinx.android.synthetic.main.prediction_image_widget.view.closeButton
import kotlinx.android.synthetic.main.prediction_image_widget.view.image_optionList
import kotlinx.android.synthetic.main.prediction_image_widget.view.questionTextView

class PollImageWidget : ConstraintLayout, WidgetObserver, QuizVoteObserver {
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimationManager
    private lateinit var resultDisplayUtil: WidgetResultDisplayUtil
    private lateinit var userTapped: () -> Unit
    private lateinit var startingState: WidgetTransientState
    private lateinit var progressedState: WidgetTransientState
    private lateinit var progressedStateCallback: (WidgetTransientState) -> Unit
    private val imageButtonMap = HashMap<View, String?>()
    private val viewOptions = HashMap<String?, ViewOption>()
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    private var fetchResult: (() -> Unit)? = null
    private var selectedOption: String? = null
    private var correctOption: String? = null
    private var timeout = 0L
    private var prevOptionSelectedId = ""
    var parentWidth = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    internal fun initialize(
        dismiss: () -> Unit,
        startingState: WidgetTransientState,
        progressedState: WidgetTransientState,
        fetch: () -> Unit,
        parentWidth: Int,
        viewAnimation: ViewAnimationManager,
        progressedStateCallback: (WidgetTransientState) -> Unit
    ) {
        this.startingState = startingState
        this.progressedState = progressedState
        this.timeout = startingState.timeout
        this.dismissWidget = dismiss
        this.fetchResult = fetch
        this.viewAnimation = viewAnimation
        this.parentWidth = parentWidth
        this.progressedStateCallback = progressedStateCallback
        inflate(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_image_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_image_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
        when {
            isWidgetDisplayedFirstTime(startingState) -> viewAnimation.startWidgetTransitionInAnimation {
                pieTimerViewStub.layoutResource = R.layout.pie_timer
                val pieTimer = pieTimerViewStub.inflate()
                startPieTimer(pieTimer, startingState)
            }
            isWidgetRestoredFromQuestionPhase(startingState) -> {
                pieTimerViewStub.layoutResource = R.layout.pie_timer
                val pieTimer = pieTimerViewStub.inflate()
                startPieTimer(pieTimer, startingState)
            }
            else -> {
                pieTimerViewStub.layoutResource = R.layout.cross_image
                pieTimerViewStub.inflate()
            }
        }

        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        image_optionList.layoutManager = linearLayoutManager

        resultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
        questionTextView.layoutParams.width = parentWidth
    }

    private fun isWidgetRestoredFromQuestionPhase(properties: WidgetTransientState) =
        properties.timerAnimatorStartPhase > 0f && properties.resultAnimatorStartPhase == 0f

    private fun isWidgetDisplayedFirstTime(properties: WidgetTransientState) =
        properties.timerAnimatorStartPhase == 0f

    private fun startPieTimer(pieTimer: View, properties: WidgetTransientState) {
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout * 2)
        viewAnimation.startTimerAnimation(pieTimer, properties.timeout, properties, {
            fetchResult?.invoke()
            disableButtons()
            closeButton.visibility = View.VISIBLE
            closeButton.setOnClickListener { dismissWidget?.invoke() }
        }, {
            progressedState.timerAnimatorStartPhase = it
            progressedStateCallback.invoke(progressedState)
        })
    }

    override fun questionUpdated(questionText: String) {
        viewAnimation.addHorizontalSwipeListener(questionTextView.apply {
            text = questionText
            isClickable = true
            background = AppCompatResources.getDrawable(context, R.drawable.poll_textview_rounded_corner)
        }, layout, dismissWidget)
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>
    ) {
        image_optionList.adapter?.let {
            Handler(Looper.getMainLooper()).post { updateVoteCount(voteOptions) }
        } ?: run { image_optionList.adapter = ImageAdapter(voteOptions, optionSelectedCallback) }
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        if (prevOptionSelectedId != selectedOption)
            fetchResult?.invoke()
        prevOptionSelectedId = selectedOption ?: ""
        selectedOption = selectedOptionId
        progressedState.userSelection = selectedOptionId
        imageButtonMap.forEach { (button, id) ->
            if (selectedOptionId == id)
                button.background = AppCompatResources.getDrawable(context, R.drawable.button_poll_answer_outline)
            else button.background = AppCompatResources.getDrawable(context, R.drawable.button_rounded_corners)
        }
    }

    override fun confirmMessageUpdated(confirmMessage: String) {
        confirmMessageTextView.text = confirmMessage
    }

    fun userTappedCallback(userTapped: () -> Unit) {
        this.userTapped = userTapped
    }

    override fun updateVoteCount(voteOptions: List<VoteOption>) {
        voteOptions.forEach { option ->
            val viewOption = viewOptions[option.id]
            if (viewOption != null) {
                viewOption.percentageTextView.visibility = View.VISIBLE
                viewOption.percentageTextView.text = option.votePercentage.toString().plus("%")
                resultDisplayUtil.updateViewDrawable(
                    option.id,
                    viewOption.progressBar,
                    viewOption.button,
                    option.votePercentage.toInt(),
                    correctOption,
                    selectedOption, true
                )
            }
        }
    }

    private fun disableButtons() {
        viewOptions.forEach { option ->
            option.value.button.setOnClickListener(null)
        }
    }

    inner class ImageAdapter(
        private val optionList: List<VoteOption>,
        private val optionSelectedCallback: (String?) -> Unit
    ) : RecyclerView.Adapter<ViewHolder>() {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = optionList[position]
            holder.optionText.text = option.description
            option.isCorrect.let { if (it) correctOption = option.id }

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
            // This is needed here as notifyDataSetChanged() is behaving asynchronously. So after device config change need
            // a way to update user selection.
            if (option == optionList[optionList.size - 1] && progressedState.userSelection != null)
                optionSelectedUpdated(progressedState.userSelection)

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
        val percentageText: TextView = view.percentageText
        val progressBar: ProgressBar = view.determinateBar
    }
}