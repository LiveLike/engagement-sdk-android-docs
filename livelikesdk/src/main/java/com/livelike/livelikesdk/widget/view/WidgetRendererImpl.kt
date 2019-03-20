package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.gson.JsonObject
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
import com.livelike.livelikesdk.widget.model.SimpleWidget
import com.livelike.livelikesdk.widget.model.Widget
import com.livelike.livelikesdk.widget.view.image.PredictionImageQuestionWidget

internal class WidgetRendererImpl(val context: Context, private val container: FrameLayout) : WidgetRenderer {

    override var widgetListener : WidgetEventListener? = null
    private var currentWidget: Widget? = null
    private lateinit var observerListeners: Set<WidgetManager.WidgetAnalyticsObserver>

    override fun displayWidget(
        type: WidgetType,
        payload: JsonObject,
        observerListeners: Set<WidgetManager.WidgetAnalyticsObserver>
    ) {
        this.observerListeners = observerListeners
        logDebug { "NOW - Show Widget ${type.value} on screen: $payload" }
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.topMargin = 0

        when (type) {
            WidgetType.TEXT_PREDICTION -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                val predictionWidget = PredictionTextQuestionWidgetView(context, null, 0) { dismissCurrentWidget() }

                predictionWidget.layoutParams = layoutParams
                val widgetData = PredictionWidgetQuestion()

                widgetData.registerObserver(predictionWidget)
                parser.parseTextPredictionCommon(widgetData, widgetResource)
                predictionWidget.userTappedCallback {
                    emitWidgetOptionSelected(widgetData.id, widgetResource.kind)
                }
                container.addView(predictionWidget)
                emitWidgetShown(widgetData.id, widgetResource.kind)
                currentWidget = widgetData
            }

            WidgetType.TEXT_PREDICTION_RESULTS -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                val predictionWidget = PredictionTextFollowUpWidgetView(context, null, 0) { dismissCurrentWidget() }

                predictionWidget.layoutParams = layoutParams
                val followupWidgetData = PredictionWidgetFollowUp()
                followupWidgetData.registerObserver(predictionWidget)
                parser.parseTextPredictionFollowup(followupWidgetData, widgetResource)
                if (followupWidgetData.optionSelected.id.isNullOrEmpty()) {
                    //user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }
                container.addView(predictionWidget)
                emitWidgetShown(followupWidgetData.id, widgetResource.kind)
                currentWidget = followupWidgetData
            }

            WidgetType.IMAGE_PREDICTION -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                val predictionWidget = PredictionImageQuestionWidget(context, null, 0)  { dismissCurrentWidget() }
                predictionWidget.layoutParams = layoutParams
                val widgetData = PredictionWidgetQuestion()
                widgetData.registerObserver(predictionWidget)
                parser.parseTextPredictionCommon(widgetData, widgetResource)
                predictionWidget.userTappedCallback {
                    emitWidgetOptionSelected(widgetData.id, widgetResource.kind)
                }
                container.addView(predictionWidget)
                emitWidgetShown(widgetData.id, widgetResource.kind)
                currentWidget = widgetData
            }

            WidgetType.ALERT -> {
                val alertWidget = AlertWidget(context, null).apply {
                    val alertResource = gson.fromJson(payload, Alert::class.java)
                    initialize({ dismissCurrentWidget() }, alertResource)
                    currentWidget = SimpleWidget().apply { id = alertResource.id }
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
        observerListeners.forEach { listener ->
            widgetId?.let { listener.widgetOptionSelected(it,kind) }
        }
    }

    private fun emitWidgetDismissed(widgetId: String?, kind: String) {
        observerListeners.forEach { listener ->
            widgetId?.let { listener.widgetDismissed(it,kind) }
        }
    }

    private fun emitWidgetShown(widgetId: String?, kind: String) {
        observerListeners.forEach { listener ->
            widgetId?.let { listener.widgetShown(it,kind) }
        }
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