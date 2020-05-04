package com.livelike.engagementsdk.widget.view.themes

import com.livelike.engagementsdk.widget.BaseTheme
import com.livelike.engagementsdk.widget.Component

data class AlertTheme(
    val body: Component? = null,
    val dismiss: Component? = null,
    val footer: Component? = null,
    val header: Component? = null,
    val timer: Component? = null,
    val title: Component? = null
) : BaseTheme() {
    override fun validate(): String? {
        return body?.validate() ?: dismiss?.validate() ?: footer?.validate() ?: header?.validate()
        ?: timer?.validate() ?: title?.validate()
    }
}