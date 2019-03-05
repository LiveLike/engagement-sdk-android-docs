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
import com.livelike.livelikesdk.util.extractLong
import com.livelike.livelikesdk.util.extractStringOrEmpty
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.WidgetEvent
import com.livelike.livelikesdk.widget.WidgetRenderer
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.PredictionWidgetFollowUp
import com.livelike.livelikesdk.widget.model.PredictionWidgetQuestion
import com.livelike.livelikesdk.widget.model.Widget
import com.livelike.livelikesdk.widget.model.WidgetOptions
import com.livelike.livelikesdk.widget.view.image.PredictionImageQuestionWidget
import java.net.URI
import java.util.*

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

        when (type) {
            WidgetType.TEXT_PREDICTION -> {
                val predictionWidget = PredictionTextQuestionWidgetView(context, null, 0) { dismissCurrentWidget() }
                predictionWidget.layoutParams = layoutParams
                val widgetData = PredictionWidgetQuestion()

                widgetData.registerObserver(predictionWidget)
                parseTextPredictionWidget(widgetData, payload)
                container.addView(predictionWidget)
                currentWidget = widgetData
            }

            WidgetType.TEXT_PREDICTION_RESULTS -> {
                val predictionWidget =
                    PredictionTextFollowUpWidgetView(context, null, 0) { dismissCurrentWidget() }
                predictionWidget.layoutParams = layoutParams
                val followupWidgetData = PredictionWidgetFollowUp()
                followupWidgetData.registerObserver(predictionWidget)
                parseTextPredictionFollowup(followupWidgetData, payload)
                if(followupWidgetData.optionSelected.id.isNullOrEmpty()) {
                    //user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }
                container.addView(predictionWidget)
                currentWidget = followupWidgetData
            }

            WidgetType.IMAGE_PREDICTION -> {
                val predictionWidget = PredictionImageQuestionWidget(context, null, 0)
                predictionWidget.layoutParams = layoutParams
                val widgetData = PredictionWidgetQuestion()
                widgetData.registerObserver(predictionWidget)
                parseTextPredictionWidget(widgetData, payload)
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

    private fun parseTextPredictionWidget(widgetData: PredictionWidgetQuestion, payload: JsonObject) {
        parseTextPredictionCommon(widgetData, payload)
        //widgetData.confirmationMessage = payload.extractStringOrEmpty("confirmation_message")
    }

    private fun parseTextPredictionFollowup(widgetData: PredictionWidgetFollowUp, payload: JsonObject) {
        parseTextPredictionCommon(widgetData, payload)
        widgetData.correctOptionId = payload.extractStringOrEmpty("correct_option_id")
        widgetData.questionWidgetId = payload.extractStringOrEmpty("text_prediction_id")

        widgetData.optionSelected = previousWidgetSelections[widgetData.questionWidgetId] ?: return
        //Are we ever going to need these again? Remove for now
        previousWidgetSelections.remove(widgetData.questionWidgetId)
    }

    private fun parseTextPredictionCommon(widgetData: Widget, payload: JsonObject) {
        widgetData.question = payload.extractStringOrEmpty("question")

        // Fixme: id for follow widget comes as id and for question widget it is missing, though there is
        // text_prediction_id, which is the id of the widget it is connected to. For now generate an ID
        var widgetId = payload.extractStringOrEmpty("id")
        if (widgetId == "")  widgetId = UUID.randomUUID().toString()
        widgetData.id = widgetId
        val options = mutableListOf<WidgetOptions>()

        for (option in payload["options"].asJsonArray) {
            val optionJson = option.asJsonObject
            options.add(
                WidgetOptions(
                    optionJson.extractStringOrEmpty("id"),
                    URI.create(optionJson.extractStringOrEmpty("vote_url")),
                    optionJson.extractStringOrEmpty("description"),
                    optionJson.extractLong("vote_count"),
                    optionJson.extractStringOrEmpty("image_url")
                )
            )
        }
        widgetData.optionList = options.toList()
    }
}

interface WidgetEventListener {
    fun onAnalyticsEvent(data: Any)
    fun onWidgetEvent(event: WidgetEvent)
    fun onOptionVote(voteUrl: String)
}