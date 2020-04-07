package com.livelike.engagementsdk.widget.view.components

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
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.logDebug
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Option
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageBar
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageButton
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageButtonBackground
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageItemRoot
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imagePercentage
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageText
import kotlinx.android.synthetic.main.atom_widget_text_item.view.bkgrd
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
        selectedPredictionId: String = "",
        itemIsLast: Boolean
    ) {
        if (!inflated) {
            if (!option.image_url.isNullOrEmpty()) {
                setupImageItem(option)
            } else {
                setupTextItem(option)
            }
            determinateBar?.progress = option.percentage
            percentageText?.text = "${option.percentage}%"
            imageBar?.progress = option.percentage
            imagePercentage?.text = "${option.percentage}%"
        }

        setItemBackground(itemIsSelected, widgetType, correctOptionId, selectedPredictionId, option, itemIsLast)
        animateProgress(option)
    }

    private fun setupTextItem(option: Option) {
        if (!inflated) {
            inflated = true
            inflate(context, R.layout.atom_widget_text_item, this)
            layoutTransition = LayoutTransition()
        }
        text_button?.text = option.description

        clickListener?.apply {
            text_button?.setOnClickListener(clickListener)
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
        option: Option,
        itemIsLast: Boolean
    ) {
        logDebug { "WidgetItemView setbackground widgetType:$widgetType , isSelected:$itemIsSelected , isItemLast:$itemIsLast" }
        if (itemIsSelected) {
            when (widgetType) { // TODO: make a set with the entire widget customization drawable and pass it from the adapter
                WidgetType.TEXT_PREDICTION, WidgetType.IMAGE_PREDICTION -> {
                    updateViewProgressBar(R.drawable.progress_bar_prediction)
                    updateViewButtonBackground(
                        R.drawable.answer_outline_selected_prediction
                    )
                }
                WidgetType.TEXT_POLL, WidgetType.IMAGE_POLL -> {
                    updateViewProgressBar(R.drawable.progress_bar_poll)
                    updateViewButtonBackground(
                        R.drawable.answer_outline_selected_poll
                    )
                }
                WidgetType.TEXT_QUIZ, WidgetType.IMAGE_QUIZ -> {
                    updateViewProgressBar(R.drawable.progress_bar_quiz)
                    updateViewButtonBackground(
                        R.drawable.answer_outline_selected_quiz
                    )
                }
                else -> {
                    updateViewProgressBar(R.drawable.progress_bar_neutral)
                    updateViewButtonBackground(
                        R.drawable.answer_outline_selected_poll
                    )
                }
            }
        } else {
            updateViewProgressBar(R.drawable.progress_bar_neutral)
            updateViewButtonBackground(R.color.livelike_transparent)
        }

        if (!correctOptionId.isNullOrEmpty()) {
            updateViewProgressBar(R.drawable.progress_bar_neutral)
            if (userSelectedOptionId == option.id && !option.is_correct) {
                updateViewProgressBar(R.drawable.progress_bar_wrong)
                updateViewButtonBackground(R.drawable.answer_outline_wrong)
            }
            if (option.is_correct) {
                updateViewProgressBar(R.drawable.progress_bar_correct)
                updateViewButtonBackground(R.drawable.answer_outline_correct)
            }
        }
        if (itemIsLast) {
            updateViewBackground(R.drawable.answer_background_last_item)
        } else {
            updateViewBackground(R.drawable.answer_background_default)
        }
        setProgressVisibility(!correctOptionId.isNullOrEmpty())
    }

    @SuppressLint("SetTextI18n")
    private fun animateProgress(option: Option) {
        val startValue = getCurrentProgress()
        if (option.percentage != startValue) { // Only animate if values are different
            ValueAnimator.ofFloat(startValue.toFloat(), option.percentage.toFloat()).apply {
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

    private fun getCurrentProgress(): Int {
        return determinateBar?.progress ?: imageBar?.progress ?: 0
    }

    private fun setupImageItem(option: Option) {
        if (!inflated) {
            inflated = true
            inflate(context, R.layout.atom_widget_image_item, this)
            layoutTransition = LayoutTransition()
        }
        imageText.text = option.description

        Glide.with(context)
            .load(option.image_url)
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
        if (determinateBar != null && determinateBar?.tag != drawableId) {
            determinateBar?.progressDrawable = drawable
            determinateBar?.tag = drawableId
        }
        if (imageBar != null && imageBar?.tag != drawableId) {
            imageBar?.progressDrawable = drawable
            determinateBar?.tag = drawableId
        }
    }

    private fun updateViewButtonBackground(drawableId: Int) {
        val drawable = AppCompatResources.getDrawable(context, drawableId)
        if (text_button != null && text_button?.tag != drawableId) {
            text_button?.background = drawable
            text_button?.tag = drawableId
        }
        if (imageButtonBackground != null && imageButtonBackground?.tag != drawableId) {
            imageButtonBackground?.background = drawable
            imageButtonBackground?.tag = drawableId
        }
    }

    private fun updateViewBackground(drawableId: Int) {
        val drawable = AppCompatResources.getDrawable(context, drawableId)
        if (bkgrd != null && bkgrd.tag != drawableId) {
            bkgrd.background = drawable
            bkgrd.tag = drawableId
        }
        if (imageItemRoot != null && imageItemRoot.tag != drawableId) {
            imageItemRoot.background = drawable
            imageItemRoot.tag = drawableId
        }
    }

    fun setProgressVisibility(b: Boolean) {
        val visibility = if (b) View.VISIBLE else View.INVISIBLE
        imagePercentage?.visibility = visibility
        imageBar?.visibility = visibility
        determinateBar?.visibility = visibility
        percentageText?.visibility = visibility
    }
}
