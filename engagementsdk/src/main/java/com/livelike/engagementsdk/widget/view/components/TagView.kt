package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.databinding.AtomWidgetTagViewBinding
import com.livelike.engagementsdk.widget.ViewStyleProps


class TagView (context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
    var tagViewBinding: AtomWidgetTagViewBinding =
        AtomWidgetTagViewBinding.inflate(LayoutInflater.from(context), this@TagView, true)

    var tag: String = ""
        set(value) {
            field = value
            tagViewBinding.tagTextView.text = value
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
