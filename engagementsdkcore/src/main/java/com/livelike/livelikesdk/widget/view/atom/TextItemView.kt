package com.livelike.livelikesdk.widget.view.atom

import android.content.Context
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.atom_widget_text_item.view.determinateBar
import kotlinx.android.synthetic.main.atom_widget_text_item.view.percentageText
import kotlinx.android.synthetic.main.atom_widget_text_item.view.text_button

class TextItemView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {
    private val DEFAULT_BACKGROUND_COLOR: Int = Color.BLACK
    private val DEFAULT_SELECTION_OUTLINE_COLOR: Int = Color.BLUE

    init {
        inflate(context, R.layout.atom_widget_text_item, this)

        val typedArray = context.theme.obtainStyledAttributes(
            attr, R.styleable.TextItemView,
            0, 0
        )

        val text = typedArray.getString(R.styleable.TextItemView_text) ?: ""
        val progress = typedArray.getInt(R.styleable.TextItemView_progress, 0)
        val backgroundColor = typedArray.getColor(R.styleable.TextItemView_backgroundColor, DEFAULT_BACKGROUND_COLOR)
        val selectionOutlineColor =
            typedArray.getColor(R.styleable.TextItemView_selectionOutineColor, DEFAULT_SELECTION_OUTLINE_COLOR)

        text_button.text = text
        determinateBar.progress = progress
        percentageText.text = "$progress%"
        text_button.setOnClickListener {
            updateViewBackground(R.drawable.button_poll_answer_outline)
        }
        setBackgroundColor(backgroundColor)
    }

    private fun updateViewBackground(drawable: Int) {
        text_button.background = AppCompatResources.getDrawable(context, drawable)
    }
}