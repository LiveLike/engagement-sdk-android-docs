package com.livelike.livelikesdk.widget.view.atom

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.widget.model.Option
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageBar
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageButton
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imagePercentage
import kotlinx.android.synthetic.main.atom_widget_image_item.view.imageText
import kotlinx.android.synthetic.main.atom_widget_text_item.view.determinateBar
import kotlinx.android.synthetic.main.atom_widget_text_item.view.percentageText
import kotlinx.android.synthetic.main.atom_widget_text_item.view.text_button

internal class TextItemView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {
    private var inflated = false
    var clickListener: OnClickListener? = null

    fun setData(option: Option) {
        if (!option.image_url.isNullOrEmpty()) {
            setupImageItem(option)
        } else {
            setupTextItem(option)
        }
    }

    // TODO: Split this in 2 classes, 2 adapters
    private fun setupTextItem(option: Option) {
        if (!inflated) {
            inflated = true
            inflate(context, R.layout.atom_widget_text_item, this)
        }
        text_button.text = option.description
        val progress = option.getPercent(option.getMergedVoteCount()) // TODO: need to get total votes from the resource
        determinateBar.progress = progress
        percentageText.text = "$progress%"
        clickListener?.apply {
            text_button.setOnClickListener(clickListener)
        }
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupImageItem(option: Option) {
        if (!inflated) {
            inflated = true
            inflate(context, R.layout.atom_widget_image_item, this)
        }
        imageText.text = option.description
        val progress = option.getPercent(option.getMergedVoteCount()) // TODO: need to get total votes from the resource
        imageBar.progress = progress
        imagePercentage.text = "$progress%"
        Glide.with(context)
            .load(option.image_url)
            .apply(
                RequestOptions().override(AndroidResource.dpToPx(80), AndroidResource.dpToPx(80))
                    .transform(MultiTransformation(FitCenter(), RoundedCorners(12)))
            )
            .into(imageButton)
        clickListener?.apply {
            imageButton.setOnClickListener(clickListener)
        }
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun updateViewBackground(drawable: Int) {
        text_button?.background = AppCompatResources.getDrawable(context, drawable)
//        imageButtonBackground?.background = AppCompatResources.getDrawable(context, drawable)
    }
}