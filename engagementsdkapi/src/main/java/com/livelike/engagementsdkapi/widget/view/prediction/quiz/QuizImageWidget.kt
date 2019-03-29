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
import com.bumptech.glide.request.RequestOptions
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.binding.WidgetObserver
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.widget.model.VoteOption
import kotlinx.android.synthetic.main.confirm_message.view.*
import kotlinx.android.synthetic.main.prediction_image_row_element.view.*
import kotlinx.android.synthetic.main.prediction_image_widget.view.*

class QuizImageWidget : ConstraintLayout, WidgetObserver {
    private var dismissWidget: (() -> Unit)? = null
    private lateinit var pieTimerViewStub: ViewStub
    private lateinit var viewAnimation: ViewAnimation
    private var layout = ConstraintLayout(context, null, 0)
    private var optionSelected = false
    private val imageButtonMap = HashMap<ImageButton, VoteOption?>()
    private val viewOptions = ArrayList<ViewOption>()
    lateinit var userTapped: () -> Unit
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

    init {
        inflate(context)
    }

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
            viewAnimation.startTimerAnimation(pieTimer, 7000)
            {
                viewAnimation.hideWidget()
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
        val imageScrollAdapter =
            ImageAdapter(voteOptions, optionSelectedCallback)
        image_optionList.adapter = imageScrollAdapter
    }

    override fun optionSelectedUpdated(selectedOptionId: String?) {
        optionSelected = true
        viewOptions.forEach{ option ->
            if (selectedOptionId == option.id) {
                option.button.background = AppCompatResources.getDrawable(context, R.drawable.button_pressed)
                option.progressBar.apply {
                    progressDrawable = AppCompatResources.getDrawable(context, R.drawable.progress_bar_user_correct)
                    visibility = View.VISIBLE
                }
                option.percentageTextView.visibility = View.VISIBLE
            } else {
                option.progressBar.apply {
                    progressDrawable = AppCompatResources.getDrawable(context, R.drawable.progress_bar_wrong_option)
                    visibility = View.VISIBLE
                }
                option.percentageTextView.visibility = View.VISIBLE
                option.button.background = AppCompatResources.getDrawable(context, R.drawable.button_rounded_corners)
            }
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
            imageButtonMap[holder.optionButton] = option
            holder.percentageText.visibility = View.INVISIBLE
            holder.progressBar.visibility = View.INVISIBLE
            holder.percentageText.text = option.answerCount.toString().plus("%")
            holder.progressBar.progress = option.votePercentage.toInt()
            viewOptions.add(ViewOption(holder.optionButton, option.id, holder.progressBar, holder.percentageText))
            holder.optionButton.setOnClickListener {
                val selectedOption = imageButtonMap[holder.optionButton]
                optionSelectedCallback(selectedOption?.id)
                userTapped.invoke()
            }
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