package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.widget.ViewStyleProps
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView

class TitleView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
    var title: String = ""
        set(value) {
            field = value
            titleTextView.text = value
        }
    var componentTheme: ViewStyleProps? = null
<<<<<<< Updated upstream
    set(value) {
        field = value
        value?.padding?.let { padding ->
            setPadding(
                AndroidResource.webPxToDevicePx(padding[0].toInt()),
                AndroidResource.webPxToDevicePx(padding[1].toInt()),
                AndroidResource.webPxToDevicePx(padding[2].toInt()),
                AndroidResource.webPxToDevicePx(padding[3].toInt())
            )
        }
    }
=======
        set(value) {
            field = value
            value?.padding?.let { padding ->
                setPadding(
                    AndroidResource.webPxToDevicePx(padding[0].toInt()),
                    AndroidResource.webPxToDevicePx(padding[1].toInt()),
                    AndroidResource.webPxToDevicePx(padding[2].toInt()),
                    AndroidResource.webPxToDevicePx(padding[3].toInt())
                )
            }
        }
>>>>>>> Stashed changes

    init {
        inflate(context, R.layout.atom_widget_title, this)
    }
}
