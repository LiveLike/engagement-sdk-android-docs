package com.livelike.livelikesdk.parser

import com.livelike.livelikesdk.widget.model.PredictionWidgetFollowUp
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.model.Widget
import com.livelike.livelikesdk.widget.model.WidgetOptions
import java.net.URI

// TODO: Refine this class more.
internal class WidgetParser {
    fun parseTextPredictionCommon(widget: Widget, resource: Resource) {
        widget.question = resource.question
        widget.confirmMessage = resource.confirmation_message
        widget.id = resource.id
        widget.optionList = resource.options.map {
            WidgetOptions(it.id, URI.create(it.vote_url), it.description, it.vote_count.toLong())
        }
    }

    fun parseTextPredictionFollowup(
        widgetFollowUp: PredictionWidgetFollowUp,
        payload: Resource,
        previousWidgetSelections: MutableMap<String, WidgetOptions?>
    ) {
        parseTextPredictionCommon(widgetFollowUp, payload)
        widgetFollowUp.correctOptionId = payload.correct_option_id
        widgetFollowUp.questionWidgetId = payload.text_prediction_id
        widgetFollowUp.optionSelected = previousWidgetSelections[widgetFollowUp.questionWidgetId] ?: return
    }
}