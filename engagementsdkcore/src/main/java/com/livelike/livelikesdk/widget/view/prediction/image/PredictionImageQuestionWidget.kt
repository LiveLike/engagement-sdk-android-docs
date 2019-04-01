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
import android.widget.ImageButton
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
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
    private lateinit var viewAnimation: ViewAnimation
    lateinit var widgetResultDisplayUtil: WidgetResultDisplayUtil
    private val widgetOpacityFactor: Float = 0.2f
    private var optionSelected = false
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    private var parentWidth = 0
    val imageButtonMap = HashMap<ImageButton, String?>()
    lateinit var userTapped: () -> Unit

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun initialize(dismiss: () -> Unit, timeout: Long, parentWidth: Int) {
        inflate(context, timeout)
        dismissWidget = dismiss
        this.parentWidth = parentWidth
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context, timeout: Long) {
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_image_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_image_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        // TODO: Maybe inject this object.
        viewAnimation = ViewAnimation(this)
        viewAnimation.startWidgetTransitionInAnimation {
            viewAnimation.startTimerAnimation(pieTimer, timeout) {
                if (optionSelected) {
                    viewAnimation.showConfirmMessage(
                        prediction_confirm_message_textView,
                        prediction_confirm_message_animation
                    ) {}
                    performPredictionWidgetFadeOutOperations()
                }
            }
        }
        widgetResultDisplayUtil = WidgetResultDisplayUtil(context, viewAnimation)
        Handler().postDelayed({viewAnimation.triggerTransitionOutAnimation { dismissWidget?.invoke() }},timeout)
    }

    private fun performPredictionWidgetFadeOutOperations() {
        imageButtonMap.forEach { (button) ->
            disableButtons(button)
            button.setTranslucent()
        }
        prediction_question_textView.setTranslucent()
        prediction_pie_updater_animation.setTranslucent()
    }

    private fun View.setTranslucent() {
        this.alpha = widgetOpacityFactor
    }

    private fun disableButtons(button: View) {
        button.isEnabled = false
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
                button.background = AppCompatResources.getDrawable(context, R.drawable.prediction_button_pressed)
            else button.background = AppCompatResources.getDrawable(context, R.drawable.button_rounded_corners)
        }
    }

    override fun confirmMessageUpdated(confirmMessage: String) {
        prediction_confirm_message_textView.text = confirmMessage
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
            val imageWidth = AndroidResource.dpToPx(74)

            // TODO: Move this to adapter layer.
            Glide.with(context)
                .load(option.imageUrl)
                .apply(RequestOptions().override(imageWidth, imageWidth))
                .into(holder.optionButton)
            imageButtonMap[holder.optionButton] = option.id
            holder.optionButton.setOnClickListener {
                val selectedOption = imageButtonMap[holder.optionButton]
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
            if (optionList.size > 2)
                view.layoutParams.width = ((parentWidth/2.5).toInt())
            else view.layoutParams.width = parentWidth/2

            return ViewHolder(
                view
            )
        }

        override fun getItemCount(): Int {
            return optionList.size
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val optionButton: ImageButton = view.image_button
        val optionText: TextView = view.item_text
    }
}