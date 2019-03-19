package com.livelike.livelikesdk.analytics

import com.livelike.livelikesdk.util.logInfo
import com.livelike.livelikesdk.widget.WidgetManager

internal class InteractionLogger : WidgetManager.WidgetAnalyticsObserver {
    override fun widgetDismissed(widgetId: String) {
        logInfo { "Interaction type Widget Dismissed $widgetId " }
    }

    override fun widgetShown(widgetId: String) {
        logInfo { "Interaction type Widget Shown $widgetId " }
    }

    override fun widgetOptionSelected(widgetId: String) {
        logInfo { "Interaction type Widget option selected $widgetId " }
    }

}