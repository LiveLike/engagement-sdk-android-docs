package com.livelike.livelikesdk.parser

import com.google.gson.JsonObject
import com.livelike.livelikesdk.util.extractLong
import com.livelike.livelikesdk.util.extractStringOrEmpty
import com.livelike.livelikesdk.widget.model.PredictionWidgetFollowUp
import com.livelike.livelikesdk.widget.model.Widget
import com.livelike.livelikesdk.widget.model.WidgetOptions
import java.net.URI
import java.util.*

// TODO: Refine this class more.
internal class WidgetParser {
    fun parseTextPredictionCommon(widget: Widget, payload: JsonObject) {
        widget.question = payload.extractStringOrEmpty("question")
        // Fixme: id for follow widget comes as id and for question widget it is missing, though there is
        // text_prediction_id, which is the id of the widget it is connected to. For now generate an ID
        var widgetId = payload.extractStringOrEmpty("id")
        if (widgetId == "") widgetId = UUID.randomUUID().toString()
        widget.id = widgetId
        val options = mutableListOf<WidgetOptions>()

        for (option in payload["options"].asJsonArray) {
            val optionJson = option.asJsonObject
            val widgetOptions = WidgetOptions(
                optionJson.extractStringOrEmpty("id"),
                URI.create(optionJson.extractStringOrEmpty("vote_url")),
                optionJson.extractStringOrEmpty("description"),
                optionJson.extractLong("vote_count")
            )
            widgetOptions.imageUrl = optionJson.extractStringOrEmpty("image_url")
            options.add(widgetOptions)
        }
        widget.optionList = options.toList()
    }

    fun parseTextPredictionFollowup(
        widgetFollowUp: PredictionWidgetFollowUp,
        payload: JsonObject,
        previousWidgetSelections: MutableMap<String, WidgetOptions?>
    ) {
        parseTextPredictionCommon(widgetFollowUp, payload)
        widgetFollowUp.correctOptionId = payload.extractStringOrEmpty("correct_option_id")
        widgetFollowUp.questionWidgetId = payload.extractStringOrEmpty("text_prediction_id")

        widgetFollowUp.optionSelected = previousWidgetSelections[widgetFollowUp.questionWidgetId] ?: return
    }
}