package com.livelike.livelikesdk.widget.view.prediction.image

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
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.util.WidgetResultDisplayUtil
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.pie_timer.view.*
import kotlinx.android.synthetic.main.prediction_image_row_element.view.*
import kotlinx.android.synthetic.main.prediction_image_widget.view.*

internal class PredictionImageQuestionWidget : ConstraintLayout, WidgetObserver {
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimationManager
    private lateinit var startingState: WidgetTransientState
    private lateinit var progressedStateCallback: (WidgetTransientState) -> Unit
    private lateinit var progressedState: WidgetTransientState
    lateinit var widgetResultDisplayUtil: WidgetResultDisplayUtil
    private val widgetOpacityFactor: Float = 0.2f
    private var optionSelected = false
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    var parentWidth = 0
    val imageButtonMap = HashMap<View, String?>()
    lateinit var userTapped: () -> Unit

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun initialize(dismiss: () -> Unit,
                   timeout: Long,
                   startingState: WidgetTransientState,
                   progressedState: WidgetTransientState,
                   parentWidth: Int,
                   viewAnimation: ViewAnimationManager,
                   state: (WidgetTransientState) -> Unit) {
        dismissWidget = dismiss
        this.viewAnimation = viewAnimation
        this.startingState = startingState
        this.parentWidth = parentWidth
        this.progressedState = progressedState
        this.progressedStateCallback = state
        inflate(context, timeout)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context, timeout: Long) {
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_image_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_image_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()

        if (startingState.timerAnimatorStartPhase != 0f && startingState.resultAnimatorStartPhase == 0f) {
            startPieTimer(pieTimer, timeout)
        }
        else if (startingState.timerAnimatorStartPhase != 0f && startingState.resultAnimatorStartPhase != 0f) {
            showConfirmMessage()
            performPredictionWidgetFadeOutOperations()
        }
        else viewAnimation.startWidgetTransitionInAnimation {
            startPieTimer(pieTimer, timeout)
        }
        widgetResultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout * 2)
        questionTextView.layoutParams.width = parentWidth
    }

    private fun startPieTimer(pieTimer: View, timeout: Long) {
        viewAnimation.startTimerAnimation(pieTimer, timeout, startingState, {
            if (optionSelected) {
                viewAnimation.showConfirmMessage(
                    confirmMessageTextView,
                    prediction_confirm_message_animation,
                    {},
                    {
                        progressedState.resultAnimatorStartPhase = it
                        progressedStateCallback.invoke(progressedState)
                    },
                    {
                        progressedState.resultAnimationPath = it
                        progressedStateCallback.invoke(progressedState)
                    },
                    startingState
                )
                performPredictionWidgetFadeOutOperations()
            }
        }, {
            progressedState.timerAnimatorStartPhase = it
            progressedStateCallback.invoke(progressedState)
        })
    }

    private fun showConfirmMessage() {
        viewAnimation.showConfirmMessage(
            confirmMessageTextView,
            prediction_confirm_message_animation,
            {},
            {
                progressedState.resultAnimatorStartPhase = it
                progressedStateCallback.invoke(progressedState)
            },
            {
                progressedState.resultAnimationPath = it
                progressedStateCallback.invoke(progressedState)
            },
            startingState
        )
    }

    private fun performPredictionWidgetFadeOutOperations() {
        imageButtonMap.forEach { (button) ->
            disableButtons(button)
            button.setTranslucent()
        }
        questionTextView.setTranslucent()
        prediction_pie_updater_animation.setTranslucent()
    }

    private fun View.setTranslucent() {
        this.alpha = widgetOpacityFactor
    }

    private fun disableButtons(button: View) {
        button.isEnabled = false
    }

    override fun questionUpdated(questionText: String) {
        viewAnimation.addHorizontalSwipeListener(questionTextView.apply {
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
        progressedState.userSelection = selectedOptionId
        imageButtonMap.forEach { (button, id) ->
            if (selectedOptionId == id)
                button.background = AppCompatResources.getDrawable(context, R.drawable.prediction_button_pressed)
            else button.background = AppCompatResources.getDrawable(context, R.drawable.button_rounded_corners)
        }
    }

    override fun confirmMessageUpdated(confirmMessage: String) {
        confirmMessageTextView.text = confirmMessage
    }

    fun userTappedCallback(userTapped: () -> Unit) {
        this.userTapped = userTapped
    }

    inner class ImageAdapter(
        private val optionList: List<VoteOption>,
        private val optionSelectedCallback: (String?) -> Unit

    ) : RecyclerView.Adapter<ViewHolder>() {

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = optionList[position]
            holder.optionText.text = option.description

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
            if (option == optionList[optionList.size -1]  && progressedState.userSelection != null)
                optionSelectedUpdated(progressedState.userSelection)

            holder.button.setOnClickListener {
                val selectedOption = imageButtonMap[holder.button]
                optionSelectedCallback(selectedOption)
                userTapped.invoke()
            }
            widgetResultDisplayUtil.setImageViewMargin(option, optionList, holder.itemView)
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
    }
}