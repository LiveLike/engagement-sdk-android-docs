package com.livelike.engagementsdk.widget.view.components

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ScaleDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.FontFamilyProvider
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.databinding.AtomWidgetImageItemBinding
import com.livelike.engagementsdk.databinding.AtomWidgetTextItemBinding
import com.livelike.engagementsdk.databinding.WidgetTextOptionSelectionBinding
import com.livelike.engagementsdk.widget.OptionsWidgetThemeComponent
import com.livelike.engagementsdk.widget.ViewStyleProps
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Option
import kotlin.math.roundToInt

internal class WidgetItemView(context: Context, attr: AttributeSet? = null) :
    ConstraintLayout(context, attr) {
    private var inflated = false
    private var imageItemBinding:AtomWidgetImageItemBinding? = null
    private var textItemBinding:AtomWidgetTextItemBinding? = null
    var clickListener: OnClickListener? = null

    fun setData(
        option: Option,
        itemIsSelected: Boolean,
        widgetType: WidgetType,
        correctOptionId: String?,
        selectedPredictionId: String = "",
        itemIsLast: Boolean,
        component: OptionsWidgetThemeComponent?,
        fontFamilyProvider: FontFamilyProvider?
    ) {
        if (!inflated) {
            if (!option.image_url.isNullOrEmpty()) {
                setupImageItem(option)
            } else {
                setupTextItem(option)
            }
            textItemBinding?.determinateBar?.progress = option.percentage
            textItemBinding?.percentageText?.text = "${option.percentage}%"
            imageItemBinding?.imageBar?.progress = option.percentage
            imageItemBinding?.imagePercentage?.text = "${option.percentage}%"
        }

        setItemBackground(
            itemIsSelected,
            widgetType,
            correctOptionId,
            selectedPredictionId,
            option,
            itemIsLast,
            component,
            fontFamilyProvider
        )
        animateProgress(option)
    }

    private fun setupTextItem(option: Option) {
        if (!inflated) {
            inflated = true
            //inflate(context, R.layout.atom_widget_text_item, this)
            textItemBinding = AtomWidgetTextItemBinding.inflate(LayoutInflater.from(context), this@WidgetItemView, true)
            layoutTransition = LayoutTransition()
        }
        textItemBinding?.textButton?.text = option.description
        textItemBinding?.textButton?.post {
            val layoutParam = textItemBinding?.determinateBar?.layoutParams as LayoutParams
            if (textItemBinding?.textButton?.lineCount!! > 1) {
                layoutParam.verticalBias = 0F
                layoutParam.setMargins(
                    layoutParam.leftMargin,
                    AndroidResource.dpToPx(5),
                    layoutParam.rightMargin,
                    0
                )
                textItemBinding?.textButton?.let{
                    it.setPadding(
                        it.paddingLeft,
                        AndroidResource.dpToPx(6),
                        it.paddingRight,
                        it.paddingBottom
                    )
                }

            } else {
                layoutParam.verticalBias = 0.5F
                layoutParam.setMargins(
                    layoutParam.leftMargin,
                    0,
                    layoutParam.rightMargin,
                    0
                )
                textItemBinding?.textButton?.let{
                    it.setPadding(
                        it.paddingLeft,
                    0,
                        it.paddingRight,
                        it.paddingBottom
                    )
                }
            }
            textItemBinding?.determinateBar?.layoutParams = layoutParam
        }
        clickListener?.apply {
            textItemBinding?.textButton?.setOnClickListener(clickListener)
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
        itemIsLast: Boolean,
        layoutPickerComponent: OptionsWidgetThemeComponent?,
        fontFamilyProvider: FontFamilyProvider?
    ) {
        logDebug { "WidgetItemView setbackground widgetType:$widgetType , isSelected:$itemIsSelected , isItemLast:$itemIsLast" }
        var optionDescTheme: ViewStyleProps?
        if (itemIsSelected) {
            optionDescTheme = layoutPickerComponent?.selectedOptionDescription
            when (widgetType) { // TODO: make a set with the entire widget customization drawable and pass it from the adapter
                WidgetType.TEXT_PREDICTION, WidgetType.IMAGE_PREDICTION -> {
                    if (layoutPickerComponent?.selectedOption != null) {
                        updateViewButtonBackground(
                            drawable2 = AndroidResource.createDrawable(
                                layoutPickerComponent.selectedOption
                            )
                        )
                    } else {
                        updateViewButtonBackground(
                            drawableId = R.drawable.answer_outline_selected_prediction
                        )
                    }
                    updateViewProgressBar(
                        drawableId = R.drawable.progress_bar_prediction,
                        component = layoutPickerComponent?.selectedOptionBar
                    )
                }
                WidgetType.TEXT_POLL, WidgetType.IMAGE_POLL -> {
                    updateViewProgressBar(
                        drawableId = R.drawable.progress_bar_poll,
                        component = layoutPickerComponent?.selectedOptionBar
                    )
                    if (layoutPickerComponent?.selectedOption != null) {
                        updateViewButtonBackground(
                            drawable2 = AndroidResource.createDrawable(
                                layoutPickerComponent.selectedOption
                            )
                        )
                    } else {
                        updateViewButtonBackground(
                            drawableId = R.drawable.answer_outline_selected_poll
                        )
                    }
                }
                WidgetType.TEXT_QUIZ, WidgetType.IMAGE_QUIZ -> {
                    updateViewProgressBar(
                        R.drawable.progress_bar_quiz,
                        component = layoutPickerComponent?.selectedOptionBar
                    )
                    if (layoutPickerComponent?.selectedOption != null) {
                        updateViewButtonBackground(
                            drawable2 = AndroidResource.createDrawable(
                                layoutPickerComponent.selectedOption
                            )
                        )
                    } else {
                        updateViewButtonBackground(
                            drawableId = R.drawable.answer_outline_selected_quiz
                        )
                    }
                }
                else -> {
                    updateViewProgressBar(
                        R.drawable.progress_bar_neutral,
                        component = layoutPickerComponent?.unselectedOptionBar
                    )
                    if (layoutPickerComponent?.unselectedOption != null) {
                        updateViewButtonBackground(
                            drawable2 = AndroidResource.createDrawable(
                                layoutPickerComponent.unselectedOption
                            )
                        )
                    } else
                        updateViewButtonBackground(R.color.livelike_transparent)
                }
            }
        } else {
            optionDescTheme = layoutPickerComponent?.unselectedOptionDescription
            updateViewProgressBar(
                R.drawable.progress_bar_neutral,
                component = layoutPickerComponent?.unselectedOptionBar
            )
            if (layoutPickerComponent?.unselectedOption != null) {
                updateViewButtonBackground(
                    drawable2 = AndroidResource.createDrawable(
                        layoutPickerComponent.unselectedOption
                    )
                )
            } else
                updateViewButtonBackground(R.color.livelike_transparent)
        }

        if (!correctOptionId.isNullOrEmpty()) {
            updateViewProgressBar(
                R.drawable.progress_bar_neutral,
                component = layoutPickerComponent?.unselectedOptionBar
            )
            optionDescTheme = layoutPickerComponent?.unselectedOptionDescription
            if (userSelectedOptionId == option.id && !option.is_correct) {
                optionDescTheme = layoutPickerComponent?.incorrectOptionDescription
                updateViewProgressBar(
                    R.drawable.progress_bar_wrong,
                    component = layoutPickerComponent?.incorrectOptionBar
                )
                if (layoutPickerComponent?.incorrectOption != null)
                    updateViewButtonBackground(
                        drawable2 = AndroidResource.createDrawable(
                            layoutPickerComponent.incorrectOption
                        )
                    )
                else
                    updateViewButtonBackground(R.drawable.answer_outline_wrong)
            }
            if (option.is_correct) {
                optionDescTheme = layoutPickerComponent?.correctOptionDescription
                updateViewProgressBar(
                    R.drawable.progress_bar_correct,
                    component = layoutPickerComponent?.correctOptionBar
                )
                if (layoutPickerComponent?.correctOption != null) {
                    updateViewButtonBackground(
                        drawable2 = AndroidResource.createDrawable(
                            layoutPickerComponent.correctOption
                        )
                    )
                } else
                    updateViewButtonBackground(R.drawable.answer_outline_correct)
            }
        }

        if (!option.image_url.isNullOrEmpty()) {
            AndroidResource.updateThemeForView(imageItemBinding?.imageText, optionDescTheme, fontFamilyProvider)
            AndroidResource.updateThemeForView(imageItemBinding?.imagePercentage, optionDescTheme, fontFamilyProvider)
        } else {
            AndroidResource.updateThemeForView(textItemBinding?.textButton, optionDescTheme, fontFamilyProvider)
            AndroidResource.updateThemeForView(textItemBinding?.percentageText, optionDescTheme, fontFamilyProvider)
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
                    textItemBinding?.determinateBar?.progress = progress
                    textItemBinding?.percentageText?.text = "$progress%"
                    imageItemBinding?.imageBar?.progress = progress
                    imageItemBinding?.imagePercentage?.text = "$progress%"
                }
                interpolator = LinearInterpolator()
                duration = 500
                start()
            }
        }
    }

    private fun getCurrentProgress(): Int {
        return textItemBinding?.determinateBar?.progress ?: imageItemBinding?.imageBar?.progress ?: 0
    }

    private fun setupImageItem(option: Option) {
        if (!inflated) {
            inflated = true
            //inflate(context, R.layout.atom_widget_image_item, this)
            imageItemBinding = AtomWidgetImageItemBinding.inflate(LayoutInflater.from(context), this@WidgetItemView, true)
            layoutTransition = LayoutTransition()
        }

        imageItemBinding?.imageText?.text = option.description

        Glide.with(context.applicationContext)
            .load(option.image_url)
            .into(imageItemBinding?.imageButton!!)
        clickListener?.apply {
            imageItemBinding?.imageItemRoot?.setOnClickListener(clickListener)
        }
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun updateDescViewTheme(isImage: Boolean, component: ViewStyleProps?) {
        component?.let {
            if (isImage) {
                AndroidResource.updateThemeForView(imageItemBinding?.imageText, it)
            } else {
                AndroidResource.updateThemeForView(textItemBinding?.textButton, it)
            }
        }
    }

    private fun updateViewProgressBar(drawableId: Int, component: ViewStyleProps? = null) {
        val drawable = AppCompatResources.getDrawable(context, drawableId)
        component?.let {
            val progressDrawable = AndroidResource.createDrawable(component)
            val layerDrawable =
                LayerDrawable(
                    arrayOf(
                        ClipDrawable(
                            progressDrawable,
                            Gravity.LEFT,
                            ClipDrawable.HORIZONTAL
                        ),
                        ScaleDrawable(progressDrawable, Gravity.LEFT,1f,0.1f)
                    )

                )
            layerDrawable.setId(1, android.R.id.progress)
            textItemBinding?.determinateBar?.progressDrawable = layerDrawable
            imageItemBinding?.imageBar?.progressDrawable = layerDrawable
        }
        if (component == null) {
            if (textItemBinding?.determinateBar != null && textItemBinding?.determinateBar?.tag != drawableId) {
                textItemBinding?.determinateBar?.progressDrawable = drawable
                textItemBinding?.determinateBar?.tag = drawableId
            }
            if (imageItemBinding?.imageBar != null && imageItemBinding?.imageBar?.tag != drawableId) {
                imageItemBinding?.imageBar?.progressDrawable = drawable
                textItemBinding?.determinateBar?.tag = drawableId
            }
        }
    }

    private fun updateViewButtonBackground(drawableId: Int? = null, drawable2: Drawable? = null) {
        val drawable = when {
            drawableId != null -> AppCompatResources.getDrawable(context, drawableId)
            else -> drawable2
        }
        drawable?.let {
            if (textItemBinding?.bkgrd != null && textItemBinding?.bkgrd?.tag != drawableId ?: drawable2) {
                textItemBinding?.bkgrd?.background = drawable
                textItemBinding?.bkgrd?.tag = drawableId ?: drawable2
            }
            if (imageItemBinding?.imageItemRoot != null && imageItemBinding?.imageItemRoot?.tag != drawableId ?: drawable2) {
                imageItemBinding?.imageItemRoot?.background = drawable
                imageItemBinding?.imageItemRoot?.tag = drawableId ?: drawable2
            }
        }
    }

    private fun updateViewBackground(drawableId: Int) {
        val drawable = AppCompatResources.getDrawable(context, drawableId)
        if (textItemBinding?.bkgrd != null && textItemBinding?.bkgrd?.tag != drawableId) {
            textItemBinding?.bkgrd?.background = drawable
            textItemBinding?.bkgrd?.tag = drawableId
        }
        if (imageItemBinding?.imageItemRoot != null && imageItemBinding?.imageItemRoot?.tag != drawableId) {
            imageItemBinding?.imageItemRoot?.background = drawable
            imageItemBinding?.imageItemRoot?.tag = drawableId
        }
    }

    fun setProgressVisibility(b: Boolean) {
        val visibility = if (b) View.VISIBLE else View.INVISIBLE
        imageItemBinding?.imagePercentage?.visibility = visibility
        imageItemBinding?.imageBar?.visibility = visibility
        textItemBinding?.determinateBar?.visibility = visibility
        textItemBinding?.percentageText?.visibility = visibility
    }
}
