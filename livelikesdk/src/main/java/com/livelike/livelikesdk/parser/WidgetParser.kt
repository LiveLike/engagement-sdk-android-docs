package com.livelike.livelikesdk.parser

import com.livelike.livelikesdk.util.liveLikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
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
        widget.kind = resource.kind
        widget.optionList = resource.options.map {
            WidgetOptions(it.id, URI.create(it.vote_url), it.description, it.vote_count.toLong(), it.image_url)
        }
    }

    fun parsePredictionFollowup(widget: Widget, payload: Resource) {
        parseTextPredictionCommon(widget, payload)
        when {
            payload.testTag == "Correct Answer" -> widget.optionSelected =
                widget.optionList.single { widgetOptions ->
                    widgetOptions.id == payload.correct_option_id
                }
            payload.testTag == "Wrong Answer" -> widget.optionSelected =
                widget.optionList.first { widgetOptions -> widgetOptions.id != payload.correct_option_id }

            payload.text_prediction_id != "" -> widget.optionSelected =
                WidgetOptions(getWidgetPredictionVotedAnswerIdOrEmpty(payload.text_prediction_id))

            else -> widget.optionSelected =
                WidgetOptions(getWidgetPredictionVotedAnswerIdOrEmpty(payload.image_prediction_id))
        }
        widget.correctOptionId = payload.correct_option_id
    }
}