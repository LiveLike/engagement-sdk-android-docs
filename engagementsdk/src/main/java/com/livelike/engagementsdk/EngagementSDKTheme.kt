package com.livelike.engagementsdk

import com.livelike.engagementsdk.widget.BaseTheme
import com.livelike.engagementsdk.widget.WidgetsTheme

internal data class EngagementSDKTheme(
    val chat: Map<String, Any?>? = null,
    val version: Double,
    val widgets: WidgetsTheme
):BaseTheme() {
    override fun validate(): String? {
        return widgets.validate()
    }
}