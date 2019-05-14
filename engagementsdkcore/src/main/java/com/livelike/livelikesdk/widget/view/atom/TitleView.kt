package com.livelike.livelikesdk.widget.view.atom

import android.content.Context
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView

class TitleView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
    private val DEFAULT_COLOR: Int = Color.BLACK
    var title: String = ""
        set(value) {
            field = value
            titleTextView.text = value
        }

    init {
        inflate(context, R.layout.atom_widget_title, this)

        val typedArray = context.theme.obtainStyledAttributes(
            attr, R.styleable.TitleView,
            0, 0
        )
        val titleText = typedArray.getString(R.styleable.TitleView_titleText) ?: ""
        val backgroundColor = typedArray.getColor(R.styleable.TitleView_backgroundColor, DEFAULT_COLOR)

        titleTextView.text = titleText
        setBackgroundColor(backgroundColor)
    }
}