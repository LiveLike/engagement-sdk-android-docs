package com.livelike.livelikesdk.analytics

interface WidgetAnalytics {
    fun widgetDismissed(widgetId: String)
    fun widgetShown(widgetId: String)
    fun widgetOptionSelected(widgetId: String)
}