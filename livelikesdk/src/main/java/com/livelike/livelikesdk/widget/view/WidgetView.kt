package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.gson.JsonObject
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.parser.WidgetParser
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.WidgetEvent
import com.livelike.livelikesdk.widget.WidgetRenderer
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.PredictionWidgetFollowUp
import com.livelike.livelikesdk.widget.model.PredictionWidgetQuestion
import com.livelike.livelikesdk.widget.model.Widget
import com.livelike.livelikesdk.widget.model.WidgetOptions
import com.livelike.livelikesdk.widget.view.image.PredictionImageQuestionWidget

class WidgetView(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs),
    WidgetRenderer {

    override var widgetListener : WidgetEventListener? = null
    private var container : FrameLayout
    private var currentWidget: Widget? = null
    private val previousWidgetSelections = mutableMapOf<String, WidgetOptions?>()

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_view, this, true)
        container = findViewById(R.id.containerView)
    }

    fun setSession(session: LiveLikeContentSession) {
        session.widgetRenderer = this
    }

    override fun displayWidget(type: WidgetType, payload: JsonObject) {
        logDebug { "NOW - Show Widget ${type.value} on screen: $payload" }
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.topMargin = 0
        val parser = WidgetParser()
        when (type) {
            WidgetType.TEXT_PREDICTION -> {
                val predictionWidget = PredictionTextQuestionWidgetView(context, null, 0) { dismissCurrentWidget() }
                predictionWidget.layoutParams = layoutParams
                val widgetData = PredictionWidgetQuestion()

                widgetData.registerObserver(predictionWidget)
                parser.parseTextPredictionCommon(widgetData, payload)
                container.addView(predictionWidget)
                currentWidget = widgetData
            }

            WidgetType.TEXT_PREDICTION_RESULTS -> {
                val predictionWidget = PredictionTextFollowUpWidgetView(context, null, 0) { dismissCurrentWidget() }
                predictionWidget.layoutParams = layoutParams
                val followupWidgetData = PredictionWidgetFollowUp()
                followupWidgetData.registerObserver(predictionWidget)
                parser.parseTextPredictionFollowup(followupWidgetData,
                    payload,
                    previousWidgetSelections)
                if (followupWidgetData.optionSelected.id.isNullOrEmpty()) {
                    //user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }
                container.addView(predictionWidget)
                currentWidget = followupWidgetData
            }

            WidgetType.IMAGE_PREDICTION -> {
                val predictionWidget = PredictionImageQuestionWidget(context, null, 0)  { dismissCurrentWidget() }
                predictionWidget.layoutParams = layoutParams
                val widgetData = PredictionWidgetQuestion()
                widgetData.registerObserver(predictionWidget)
                parser.parseTextPredictionCommon(widgetData, payload)
                container.addView(predictionWidget)
            }

            else -> {
            }
        }
    }

    override fun dismissCurrentWidget() {
        container.removeAllViews()
        val widget = currentWidget ?: return
        previousWidgetSelections[widget.id ?: ""] = widget.optionSelected
        val voteUrl = widget.optionSelected.voteUrl.toString()
        widgetListener?.onOptionVote(voteUrl)
        currentWidget = null
        widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
    }
}

interface WidgetEventListener {
    fun onAnalyticsEvent(data: Any)
    fun onWidgetEvent(event: WidgetEvent)
    fun onOptionVote(voteUrl: String)
}