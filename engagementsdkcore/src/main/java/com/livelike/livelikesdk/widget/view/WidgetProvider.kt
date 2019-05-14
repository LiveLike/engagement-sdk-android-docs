package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.view.View
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.view.organism.PredictionTextView

/**
 * Provides a new Widget View based on the widget Type.
 *
 */
internal class WidgetViewProvider {
    fun get(widgetType: WidgetType, context: Context): View {
        return when (widgetType) {
            WidgetType.ALERT -> AlertWidgetView(context)
            else -> PredictionTextView(context)
        }
    }
}