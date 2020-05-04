package com.livelike.engagementsdk.widget.view.themes

import com.livelike.engagementsdk.widget.BaseTheme
import com.livelike.engagementsdk.widget.Component

data class CheerMeterTheme(
    val body: Component? = null,
    val dismiss: Component? = null,
    val footer: Component? = null,
    val header: Component? = null,
    val sideABar: Component? = null,
    val sideAButton: Component? = null,
    val sideBBar: Component? = null,
    val sideBButton: Component? = null,
    val timer: Component? = null,
    val title: Component? = null,
    val versus: Component? = null
) : BaseTheme() {
    override fun validate(): String? {
        return body?.validate() ?: dismiss?.validate() ?: footer?.validate() ?: header?.validate()
        ?: sideABar?.validate() ?: sideAButton?.validate() ?: sideBBar?.validate()
        ?: sideAButton?.validate() ?: timer?.validate() ?: title?.validate() ?: versus?.validate()
    }
}