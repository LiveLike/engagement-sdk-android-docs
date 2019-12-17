package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import com.livelike.engagementsdk.R
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView

class TitleView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
    var title: String = ""
        set(value) {
            field = value
            titleTextView.text = value
        }

    init {
        inflate(context, R.layout.atom_widget_title, this)
    }
}
