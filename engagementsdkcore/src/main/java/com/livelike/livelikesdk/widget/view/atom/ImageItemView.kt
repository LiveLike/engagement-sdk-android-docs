package com.livelike.livelikesdk.widget.view.atom

import android.content.Context
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.widget.model.Option
import kotlinx.android.synthetic.main.atom_widget_text_item.view.determinateBar
import kotlinx.android.synthetic.main.atom_widget_text_item.view.percentageText
import kotlinx.android.synthetic.main.atom_widget_text_item.view.text_button

internal class ImageItemView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {
    private val DEFAULT_BACKGROUND_COLOR: Int = Color.BLACK

    init {
        inflate(context, R.layout.atom_widget_image_item, this)
        setBackgroundColor(DEFAULT_BACKGROUND_COLOR)
    }

    fun setData(option: Option) {
        text_button.text = option.description

        val progress = option.getPercentVote(option.vote_count.toFloat())
        determinateBar.progress = progress
        percentageText.text = "$progress%"
    }

    fun updateViewBackground(drawable: Int) {
        text_button.background = AppCompatResources.getDrawable(context, drawable)
    }
}