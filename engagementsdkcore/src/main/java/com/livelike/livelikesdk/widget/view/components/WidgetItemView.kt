package com.livelike.livelikesdk.widget.view.components

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Option
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageBar
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageButton
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageButtonBackground
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imagePercentage
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageText
import kotlinx.android.synthetic.main.atom_widget_text_item.view.determinateBar
import kotlinx.android.synthetic.main.atom_widget_text_item.view.percentageText
import kotlinx.android.synthetic.main.atom_widget_text_item.view.text_button
import kotlin.math.roundToInt

internal class WidgetItemView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {
    private var inflated = false
    var clickListener: OnClickListener? = null

    fun setData(
        option: Option,
        itemIsSelected: Boolean,
        widgetType: WidgetType,
        correctOptionId: String?,
        selectedPredictionId: String = ""
    ) {
        if (!option.image_url.isNullOrEmpty()) {
            setupImageItem(option)
        } else {
            setupTextItem(option)
        }
        setItemBackground(itemIsSelected, widgetType, correctOptionId, selectedPredictionId, option)
    }

    // TODO: Split this in 2 classes, 2 adapters
    private fun setupTextItem(option: Option) {
        if (!inflated) {
            inflated = true
            inflate(context, R.layout.atom_widget_text_item, this)
            layoutTransition = LayoutTransition()
        }
        text_button.text = option.description

        animateProgress(option, determinateBar.progress.toFloat())

        clickListener?.apply {
            text_button.setOnClickListener(clickListener)
        }
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setItemBackground(
        itemIsSelected: Boolean,
        widgetType: WidgetType,
        correctOptionId: String?,
        userSelectedOptionId: String,
        option: Option
    ) {
        if (itemIsSelected) {
            when (widgetType) { // TODO: make a set with the entire widget customization drawable and pass it from the adapter
                WidgetType.TEXT_PREDICTION, WidgetType.IMAGE_PREDICTION -> {
                    updateViewBackground(R.drawable.prediction_button_pressed)
                }
                WidgetType.TEXT_POLL, WidgetType.IMAGE_POLL -> {
                    updateViewProgressBar(R.drawable.progress_bar_user_selection_poll)
                    updateViewBackground(R.drawable.button_poll_answer_outline)
                }
                WidgetType.TEXT_QUIZ, WidgetType.IMAGE_QUIZ -> {
                    updateViewProgressBar(R.drawable.progress_bar_user_selection_quiz)
                    updateViewBackground(R.drawable.quiz_button_pressed)
                }
                else -> {
                    updateViewProgressBar(R.drawable.progress_bar_user_selection_neutral)
                    updateViewBackground(R.drawable.button_poll_answer_outline)
                }
            }
        } else {
            updateViewProgressBar(R.drawable.progress_bar_user_selection_neutral)
            updateViewBackground(R.color.livelike_transparent)
        }

        if (!correctOptionId.isNullOrEmpty()) {
            updateViewProgressBar(R.drawable.progress_bar_user_selection_neutral)
            if (userSelectedOptionId == option.id) {
                updateViewProgressBar(R.drawable.progress_bar_user_selection_wrong)
                updateViewBackground(R.drawable.button_wrong_answer_outline)
            }
            if (correctOptionId == option.id) {
                updateViewProgressBar(R.drawable.progress_bar_user_correct)
                updateViewBackground(R.drawable.button_correct_answer_outline)
            }
        }
        setProgressVisibility(!correctOptionId.isNullOrEmpty())
    }

    @SuppressLint("SetTextI18n")
    private fun animateProgress(option: Option, startValue: Float) {
        if(option.percentage.toFloat() != startValue){ // Only animate if values are different
            ValueAnimator.ofFloat(startValue, option.percentage.toFloat()).apply {
                addUpdateListener {
                    val progress = (it.animatedValue as Float).roundToInt()
                    determinateBar?.progress = progress
                    percentageText?.text = "$progress%"
                    imageBar?.progress = progress
                    imagePercentage?.text = "$progress%"
                }
                interpolator = LinearInterpolator()
                duration = 500
                start()
            }
        }
    }

    private fun setupImageItem(option: Option) {
        if (!inflated) {
            inflated = true
            inflate(context, R.layout.atom_widget_image_item, this)
            layoutTransition = LayoutTransition()
        }
        imageText.text = option.description

        animateProgress(option, imageBar.progress.toFloat())

        Glide.with(context)
            .load(option.image_url)
            .apply(
                RequestOptions().override(AndroidResource.dpToPx(80), AndroidResource.dpToPx(80))
                    .transform(MultiTransformation(FitCenter(), RoundedCorners(12)))
            )
            .into(imageButton)
        clickListener?.apply {
            imageButtonBackground.setOnClickListener(clickListener)
        }
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun updateViewProgressBar(drawableId: Int) {
        val drawable = AppCompatResources.getDrawable(context, drawableId)
        if(determinateBar?.progressDrawable != drawable){
            determinateBar?.progressDrawable = AppCompatResources.getDrawable(context, drawableId)
        }
        if(imageBar?.progressDrawable != drawable){
            imageBar?.progressDrawable = AppCompatResources.getDrawable(context, drawableId)
        }
    }

    private fun updateViewBackground(drawableId: Int) {
        val drawable = AppCompatResources.getDrawable(context, drawableId)
        if(text_button?.background != drawable){
            text_button?.background = AppCompatResources.getDrawable(context, drawableId)
        }
        if(imageButtonBackground?.background != drawable){
            imageButtonBackground?.background = AppCompatResources.getDrawable(context, drawableId)
        }
    }

    fun setProgressVisibility(b: Boolean) {
        val visibility = if (b) View.VISIBLE else View.GONE
        imagePercentage?.visibility = visibility
        imageBar?.visibility = visibility
        determinateBar?.visibility = visibility
        percentageText?.visibility = visibility
    }
}