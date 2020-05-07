package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.graphics.Typeface
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.widget.Component
import com.livelike.engagementsdk.widget.FontWeight
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView

class TitleView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
    var title: String = ""
        set(value) {
            field = value
            titleTextView.text = value
        }
    var componentTheme: Component? = null
    set(value) {
        field = value
        AndroidResource.updateThemeForView(titleTextView,value)
        value?.padding?.let { padding ->
            setPadding(
                AndroidResource.dpToPx(padding[0].toInt()),
                AndroidResource.dpToPx(padding[1].toInt()),
                AndroidResource.dpToPx(padding[2].toInt()),
                AndroidResource.dpToPx(padding[3].toInt())
            )
        }
    }

    init {
        inflate(context, R.layout.atom_widget_title, this)
    }

}
