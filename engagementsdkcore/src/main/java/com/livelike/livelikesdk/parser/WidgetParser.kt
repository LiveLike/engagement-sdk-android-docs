package com.livelike.livelikesdk.parser

import com.livelike.livelikesdk.util.AndroidResource.Companion.parseDuration
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.model.Widget
import com.livelike.livelikesdk.widget.model.WidgetOptions
import java.net.URI

// TODO: Refine this class more.
internal class WidgetParser {
    fun parseTextPredictionCommon(widget: Widget, resource: Resource) {
        mapResourceToWidget(widget, resource)

        widget.optionList = resource.options.map {
            WidgetOptions(
                it.id,
                URI.create(it.vote_url),
                it.description,
                it.vote_count.toLong(),
                it.image_url,
                it.answer_count.toLong(),
                it.answer_url
            )
        }
        widget.timeout = parseDuration(resource.timeout)
    }

    private fun mapResourceToWidget(
        widget: Widget,
        resource: Resource
    ) {
        widget.question = resource.question
        widget.confirmMessage = resource.confirmation_message
        widget.id = resource.id
        widget.kind = resource.kind
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

    fun parseQuiz(widget: Widget, resource: Resource) {
        mapResourceToWidget(widget, resource)
        widget.subscribeChannel = resource.subscribe_channel

        widget.optionList = resource.choices.map {
            WidgetOptions(
                it.id,
                null,
                it.description,
                it.vote_count.toLong(),
                it.image_url,
                it.answer_count.toLong(),
                it.answer_url,
                it.is_correct
            )
        }
    }

    fun parseQuizResult(widget: Widget, resource: Resource) {
        widget.optionList.forEach { option ->
            resource.choices.forEach { choice ->
                if (option.id == choice.id)
                    option.answerCount = choice.answer_count.toLong()
                if(option.isCorrect)
                    widget.correctOptionId = option.id
            }
        }
        widget.optionSelected =
            WidgetOptions(getWidgetPredictionVotedAnswerIdOrEmpty(widget.id?:""))
    }
}