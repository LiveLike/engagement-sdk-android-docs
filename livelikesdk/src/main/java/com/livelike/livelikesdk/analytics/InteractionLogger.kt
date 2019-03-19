package com.livelike.livelikesdk.analytics

import com.livelike.livelikesdk.util.logInfo
import com.livelike.livelikesdk.widget.WidgetManager

internal class InteractionLogger : WidgetManager.WidgetAnalyticsObserver {
    override fun widgetDismissed(widgetId: String, kind: String) {
        logInfo { "Interaction type Widget Dismissed $widgetId " }
        analyticService.trackWidgetDismiss(widgetId, kind)
    }

    override fun widgetShown(widgetId: String, kind: String) {
        logInfo { "Interaction type Widget Shown $widgetId " }
        analyticService.trackWidgetReceived(widgetId, kind)
    }

    override fun widgetOptionSelected(widgetId: String, kind: String) {
        logInfo { "Interaction type Widget option selected $widgetId " }
        analyticService.trackInteraction(widgetId, kind, "OptionSelected")
    }

}