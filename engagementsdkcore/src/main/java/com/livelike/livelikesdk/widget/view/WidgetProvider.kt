package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.view.organism.PredictionTextView
import com.livelike.livelikesdk.widget.view.prediction.text.TextOptionWidgetViewModel

/**
 * Provides the WidgetViewModel with initialized data.
 *
 */
internal class WidgetViewModelInitializer {
    fun get(widgetType: WidgetType, payload: JsonObject, session: LiveLikeContentSession) {
        (
                when (widgetType) {
                    WidgetType.ALERT -> ViewModelProviders.of(session.widgetContext as AppCompatActivity).get(
                        AlertWidgetViewModel::class.java
                    ) as LiveLikeViewModel
                    WidgetType.TEXT_PREDICTION, WidgetType.TEXT_PREDICTION_RESULTS -> ViewModelProviders.of(session.widgetContext as AppCompatActivity).get(
                        TextOptionWidgetViewModel::class.java
                    ) as LiveLikeViewModel
                    else -> error("Unknown widget type: " + widgetType.value)
                }).apply {
            this.payload = payload
            this.session = session
        }
    }
}

/**
 * Provides a new Widget View based on the widget Type.
 *
 */
internal class WidgetViewProvider {
    fun get(widgetType: WidgetType, context: Context): View {
        return when (widgetType) {
            WidgetType.ALERT -> AlertWidgetView(context)
            WidgetType.TEXT_PREDICTION, WidgetType.TEXT_PREDICTION_RESULTS -> PredictionTextView(context)
            else -> error("Unknown widget type: " + widgetType.value)
        }
    }
}