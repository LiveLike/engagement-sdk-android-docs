package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.databinding.AtomWidgetTitleBinding
import com.livelike.engagementsdk.widget.ViewStyleProps

class TitleView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
     var titleViewBinding: AtomWidgetTitleBinding =
        AtomWidgetTitleBinding.inflate(LayoutInflater.from(context), this@TitleView, true)

    var title: String = ""
        set(value) {
            field = value
            titleViewBinding.titleTextView.text = value
        }
    var componentTheme: ViewStyleProps? = null
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
}
