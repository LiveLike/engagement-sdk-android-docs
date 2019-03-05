package com.livelike.livelikesdk.widget.view.image

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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.AnimationHandler
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.ViewAnimation
import kotlinx.android.synthetic.main.pie_timer.view.*
import kotlinx.android.synthetic.main.piechart.view.*
import kotlinx.android.synthetic.main.prediction_image_row_element.view.*
import kotlinx.android.synthetic.main.prediction_image_widget.view.*

class PredictionImageQuestionWidget : ConstraintLayout, WidgetObserver {
    private lateinit var pieTimerViewStub : ViewStub
    private val widgetOpacityFactor: Float = 0.2f
    private val animationHandler = AnimationHandler()
    private var optionSelected = false
    private val imageButtonMap = HashMap<ImageButton, String?>()
    private var layout = ConstraintLayout(context, null, 0)

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init { inflate(context) }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflate(context: Context) {
        LayoutInflater.from(context)
            .inflate(R.layout.prediction_image_widget, this, true) as ConstraintLayout
        layout = findViewById(R.id.prediction_image_widget)
        pieTimerViewStub = findViewById(R.id.prediction_pie)
        pieTimerViewStub.layoutResource = R.layout.pie_timer
        val pieTimer = pieTimerViewStub.inflate()
        val viewAnimation = ViewAnimation(this, animationHandler)
        viewAnimation.startWidgetTransitionInAnimation()
        viewAnimation.startTimerAnimation(pieTimer, 7000) {
            if (optionSelected) {
                viewAnimation.showConfirmMessage(prediction_confirm_message_textView, prediction_confirm_message_animation)
                performPredictionWidgetFadeOutOperations()
            } else {
                viewAnimation.hideWidget()
            }
        }
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
        prediction_question_textView.apply {
            text = questionText
            isClickable = true
        }
    }

    override fun optionListUpdated(voteOptions: List<VoteOption>,
                                   optionSelectedCallback: (String?) -> Unit,
                                   correctOptionWithUserSelection: Pair<String?, String?>) {
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        image_optionList.layoutManager = linearLayoutManager
        image_optionList.adapter = ImageAdapter(voteOptions, context, optionSelectedCallback, imageButtonMap)
    }

    // TODO: This code is duplicate of Text prediction.
    override fun optionSelectedUpdated(selectedOptionId: String?) {
        optionSelected = true
        imageButtonMap.forEach { (button, id) ->
            if (selectedOptionId == id)
                button.background = AppCompatResources.getDrawable(context, R.drawable.button_pressed)
            else button.background = AppCompatResources.getDrawable(context, R.drawable.button_default)
        }
    }
}

class ImageAdapter(private val optionList: List<VoteOption>,
                   private val context: Context,
                   private val optionSelectedCallback: (String?) -> Unit,
                   private val imageButtonMap: HashMap<ImageButton, String?>) : RecyclerView.Adapter<ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = optionList[position]
        holder.optionText.text = option.description
        val imageWidth = AndroidResource.dpToPx(74)
        Glide.with(context)
            .load(option.imageUrl)
            .apply(RequestOptions().override(imageWidth, imageWidth))
            .into(holder.optionButton)

        imageButtonMap[holder.optionButton] = option.id

        holder.optionButton.setOnClickListener {
            val selectedOption = imageButtonMap[holder.optionButton]
            optionSelectedCallback(selectedOption)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.prediction_image_row_element, parent, false))
    }

    override fun getItemCount(): Int {
        return optionList.size
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val optionButton = view.image_button!!
    val optionText = view.item_text!!
}