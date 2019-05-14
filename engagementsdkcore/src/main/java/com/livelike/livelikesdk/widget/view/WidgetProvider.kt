package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.view.View
import com.livelike.livelikesdk.util.logWarn
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
            WidgetType.TEXT_PREDICTION, WidgetType.TEXT_PREDICTION_RESULTS, WidgetType.IMAGE_PREDICTION, WidgetType.IMAGE_PREDICTION_RESULTS -> PredictionTextView(
                context
            )
            else -> logWarn { "Unknown widget type: " + widgetType.value }.let { View(context) }
        }
    }
}