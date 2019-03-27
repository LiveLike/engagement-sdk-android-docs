package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.gson.JsonObject
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.analytics.analyticService
import com.livelike.livelikesdk.parser.WidgetParser
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.addWidgetPredictionVoted
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.util.logVerbose
import com.livelike.livelikesdk.widget.WidgetEvent
import com.livelike.livelikesdk.widget.WidgetManager
import com.livelike.livelikesdk.widget.WidgetRenderer
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Alert
import com.livelike.livelikesdk.widget.model.PredictionWidgetFollowUp
import com.livelike.livelikesdk.widget.model.PredictionWidgetQuestion
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.model.Widget
import com.livelike.livelikesdk.widget.view.prediction.image.PredictionImageFollowupWidget
import com.livelike.livelikesdk.widget.view.prediction.image.PredictionImageQuestionWidget
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextFollowUpWidgetView
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextQuestionWidgetView


open class WidgetView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs),
    WidgetRenderer {
    override var widgetListener : WidgetEventListener? = null
    private var container : FrameLayout
    private var currentWidget: Widget? = null
    private lateinit var observerListeners: Set<WidgetManager.WidgetAnalyticsObserver>

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_view, this, true)
        container = findViewById(R.id.containerView)
    }

    fun setSession(session: LiveLikeContentSession) {
        session.widgetRenderer = this
    }

    override fun displayWidget(
        type: String,
        payload: JsonObject
    ) {
        this.observerListeners = observerListeners
        logDebug { "NOW - Show Widget $type on screen: $payload" }
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.topMargin = 0
        val widget = Widget()
        when (WidgetType.fromString(type)) {
            WidgetType.TEXT_PREDICTION -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                val predictionWidget =
                    PredictionTextQuestionWidgetView(
                        context,
                        null,
                        0
                    ) { dismissCurrentWidget() }

                predictionWidget.layoutParams = layoutParams
                parser.parseTextPredictionCommon(widget, widgetResource)
                val widgetData = PredictionWidgetQuestion(widget)
                widget.registerObserver(predictionWidget)
                widgetData.notifyDataSetChange()
                predictionWidget.userTappedCallback {
                    emitWidgetOptionSelected(widgetData.id, widgetResource.kind)
                }
                container.addView(predictionWidget)
                emitWidgetShown(widgetData.id, widgetResource.kind)
                currentWidget = widget
            }

            WidgetType.TEXT_PREDICTION_RESULTS -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                val predictionWidget = PredictionTextFollowUpWidgetView(context, null, 0) { dismissCurrentWidget() }

                predictionWidget.layoutParams = layoutParams

                parser.parsePredictionFollowup(widget, widgetResource)
                val followupWidgetData = PredictionWidgetFollowUp(widget)
                widget.registerObserver(predictionWidget)
                followupWidgetData.notifyDataSetChange()
                if (widget.optionSelected.id.isNullOrEmpty()) {
                    //user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }
                container.addView(predictionWidget)
                emitWidgetShown(widget.id, widgetResource.kind)
                currentWidget = widget
            }

            WidgetType.IMAGE_PREDICTION -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                val predictionWidget = PredictionImageQuestionWidget(context, null, 0)  { dismissCurrentWidget() }
                predictionWidget.layoutParams = layoutParams
                val widgetData = PredictionWidgetQuestion(widget)
                parser.parseTextPredictionCommon(widget, widgetResource)
                widget.registerObserver(predictionWidget)
                widgetData.notifyDataSetChange()
                predictionWidget.userTappedCallback {
                    emitWidgetOptionSelected(widgetData.id, widgetResource.kind)
                }
                container.addView(predictionWidget)
                emitWidgetShown(widgetData.id, widgetResource.kind)
                currentWidget = widget
            }

            WidgetType.IMAGE_PREDICTION_RESULTS -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                val predictionWidget = PredictionImageFollowupWidget(context, null, 0) { dismissCurrentWidget() }

                predictionWidget.layoutParams = layoutParams

                parser.parsePredictionFollowup(widget, widgetResource)
                val followupWidgetData = PredictionWidgetFollowUp(widget)
                widget.registerObserver(predictionWidget)
                followupWidgetData.notifyDataSetChange()
                if (widget.optionSelected.id.isNullOrEmpty()) {
                    //user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }
                container.addView(predictionWidget)
                emitWidgetShown(widget.id, widgetResource.kind)
                currentWidget = widget
            }

            WidgetType.ALERT -> {
                val alertWidget = AlertWidget(context, null).apply {
                    val alertResource = gson.fromJson(payload, Alert::class.java)
                    initialize({ dismissCurrentWidget() }, alertResource)
                    currentWidget = Widget().apply { id = alertResource.id }
                    emitWidgetShown(alertResource.id, alertResource.kind)
                }
                container.addView(alertWidget)
            }

            else -> {
                logDebug { "Received Widget is not Implemented." }
            }
        }
    }

    private fun emitWidgetOptionSelected(widgetId: String?, kind: String) {
        analyticService.trackInteraction(widgetId ?: "", kind, "OptionSelected")
    }

    private fun emitWidgetDismissed(widgetId: String?, kind: String) {
        analyticService.trackWidgetDismiss(widgetId ?: "", kind)
    }

    private fun emitWidgetShown(widgetId: String?, kind: String) {
        analyticService.trackWidgetReceived(widgetId ?: "", kind)
    }

    override fun dismissCurrentWidget() {
        logVerbose { "Dismissing the widget: ${currentWidget?.id ?: "empty ID"}" }
        container.removeAllViews()
        val widget = currentWidget ?: return
        widget.id?.let { widget.optionSelected.id?.let { optionId -> addWidgetPredictionVoted(it, optionId) } }
        emitWidgetDismissed(widget.id, widget.kind ?: "unknown")
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