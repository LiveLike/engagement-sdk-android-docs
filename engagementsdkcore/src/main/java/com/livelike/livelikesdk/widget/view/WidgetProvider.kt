package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.widget.WidgetType

/**
 * Provides the WidgetViewModel with initialized data.
 *
 */
internal class WidgetViewModelInitializer {
    fun get(widgetType: WidgetType, payload: JsonObject, session: LiveLikeContentSession) {
        when (widgetType) {
            WidgetType.ALERT -> {
                ViewModelProviders.of(session.widgetContext as AppCompatActivity)
                    .get(AlertWidgetViewModel::class.java)
                    .apply {
                        this.payload = payload
                        this.session = session
                    }
            }
            else -> error("Unknown widget type: " + widgetType.value)
        }

//                WidgetType.TEXT_POLL -> PollTextWidget(context)
//                WidgetType.TEXT_PREDICTION -> View(context)
//                WidgetType.TEXT_PREDICTION_RESULTS -> PredictionTextFollowUpWidgetView(context)
//                WidgetType.IMAGE_PREDICTION -> PredictionImageQuestionWidget(context)
//                WidgetType.IMAGE_PREDICTION_RESULTS -> PredictionImageFollowupWidget(context)
//                WidgetType.TEXT_QUIZ -> View(context)
//                WidgetType.TEXT_QUIZ_RESULT -> View(context)
//                WidgetType.IMAGE_QUIZ -> View(context)
//                WidgetType.IMAGE_QUIZ_RESULT -> View(context)
//                WidgetType.TEXT_POLL_RESULT -> View(context)
//                WidgetType.IMAGE_POLL -> PollImageWidget(context)
//                WidgetType.IMAGE_POLL_RESULT -> View(context)
//                else -> {
//                    logDebug { "Received Widget is not Implemented." }
//                }
//            }
    }
}

/**
 * Provides a new Widget View based on the widget Type.
 *
 */
internal class WidgetViewProvider {
    fun get(widgetType: WidgetType, context: Context): View {
        return when (widgetType) {
            WidgetType.ALERT -> {
                AlertWidgetView(context)
            }
            else -> error("Unknown widget type: " + widgetType.value)
        }
    }
}