package com.livelike.livelikesdk.widget

import android.content.Context
import android.view.View
import com.livelike.livelikesdk.widget.WidgetType.ALERT
import com.livelike.livelikesdk.widget.WidgetType.IMAGE_POLL
import com.livelike.livelikesdk.widget.WidgetType.IMAGE_PREDICTION
import com.livelike.livelikesdk.widget.WidgetType.IMAGE_PREDICTION_FOLLOW_UP
import com.livelike.livelikesdk.widget.WidgetType.IMAGE_QUIZ
import com.livelike.livelikesdk.widget.WidgetType.TEXT_POLL
import com.livelike.livelikesdk.widget.WidgetType.TEXT_PREDICTION
import com.livelike.livelikesdk.widget.WidgetType.TEXT_PREDICTION_FOLLOW_UP
import com.livelike.livelikesdk.widget.WidgetType.TEXT_QUIZ
import com.livelike.livelikesdk.widget.view.AlertWidgetView
import com.livelike.livelikesdk.widget.view.PollView
import com.livelike.livelikesdk.widget.view.PredictionView
import com.livelike.livelikesdk.widget.view.QuizView

/**
 * Provides a new WidgetInfos View based on the widget Type.
 *
 */
internal class WidgetViewProvider {
    fun get(widgetType: WidgetType, context: Context): View? {
        return when (widgetType) {
            ALERT -> AlertWidgetView(context)
            IMAGE_PREDICTION, IMAGE_PREDICTION_FOLLOW_UP,
            TEXT_PREDICTION, TEXT_PREDICTION_FOLLOW_UP -> PredictionView(context)
            TEXT_POLL, IMAGE_POLL -> PollView(context)
            TEXT_QUIZ, IMAGE_QUIZ -> QuizView(context)
            else -> null
        }
    }
}