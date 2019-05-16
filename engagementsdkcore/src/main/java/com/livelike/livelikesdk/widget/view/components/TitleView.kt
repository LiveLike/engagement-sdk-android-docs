package com.livelike.livelikesdk.widget.view.components

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView

class TitleView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
    var title: String = ""
        set(value) {
            field = value
            titleTextView.text = value
        }

    var background: Int = R.drawable.prediciton_rounded_corner
        set(value) {
            field = value
            titleTextView.background = AppCompatResources.getDrawable(context, value)
        }

    init {
        inflate(context, R.layout.atom_widget_title, this)
    }
}