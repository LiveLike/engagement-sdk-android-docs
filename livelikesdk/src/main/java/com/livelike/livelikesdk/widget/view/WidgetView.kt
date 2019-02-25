package com.livelike.livelikesdk.widget.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.widget.WidgetEventListener
import com.livelike.livelikesdk.widget.WidgetRenderer
import com.livelike.livelikesdk.util.WidgetTestData
import com.livelike.livelikesdk.widget.WidgetEvent
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.PredictionWidgetFollowUpData
import com.livelike.livelikesdk.widget.model.PredictionWidgetQuestionData
import com.livelike.livelikesdk.widget.model.WidgetData

class WidgetView(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs),
    WidgetRenderer {

    override var widgetListener : WidgetEventListener? = null
    private var container : FrameLayout

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_view, this, true)
        container = findViewById(R.id.containerView)
    }

    fun setSession(liveLikeContentSession: LiveLikeContentSession) {
        liveLikeContentSession.renderer = this
    }

    override fun displayWidget(type: WidgetType, widgetData: WidgetData) {
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.topMargin = 0

        when (type) {
            WidgetType.TEXT_PREDICTION -> {
                val predictionWidget = PredictionTextQuestionWidgetView(context, null, 0)
                predictionWidget.layoutParams = layoutParams
                widgetData.registerObserver(predictionWidget)
                container.addView(predictionWidget)
            }
            WidgetType.TEXT_PREDICTION_RESULTS -> {
                val predictionWidget =
                    PredictionTextFollowUpWidgetView(context, null, 0)
                predictionWidget.layoutParams = layoutParams
                val followupWidgetData = PredictionWidgetFollowUpData(WidgetTestData.questionWidgetDataList)
                followupWidgetData.registerObserver(predictionWidget)
                WidgetTestData.fillDummyDataInTextPredictionFollowUpData(followupWidgetData)
                container.addView(predictionWidget)
            }
            else -> {
            }
        }
        widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
    }

    override fun dismissCurrentWidget() {
        container.removeAllViews() // TODO: Use the dismiss method when MSDK-103 is implemented
        widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
    }
}