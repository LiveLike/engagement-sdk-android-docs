package com.livelike.livelikesdk.widget

import android.content.Context
import android.view.View
import com.livelike.livelikesdk.widget.WidgetType.ALERT
import com.livelike.livelikesdk.widget.WidgetType.IMAGE_PREDICTION
import com.livelike.livelikesdk.widget.WidgetType.IMAGE_PREDICTION_RESULTS
import com.livelike.livelikesdk.widget.WidgetType.TEXT_PREDICTION
import com.livelike.livelikesdk.widget.WidgetType.TEXT_PREDICTION_RESULTS
import com.livelike.livelikesdk.widget.view.AlertWidgetView
import com.livelike.livelikesdk.widget.view.PredictionView

/**
 * Provides a new Widget View based on the widget Type.
 *
 */
internal class WidgetViewProvider {
    fun get(widgetType: WidgetType, context: Context): View? {
        return when (widgetType) {
            ALERT -> AlertWidgetView(context)
            IMAGE_PREDICTION, IMAGE_PREDICTION_RESULTS, TEXT_PREDICTION, TEXT_PREDICTION_RESULTS -> PredictionView(
                context
            )
            else -> null
        }
    }
}