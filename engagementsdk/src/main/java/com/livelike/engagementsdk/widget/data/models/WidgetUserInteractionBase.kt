package com.livelike.engagementsdk.widget.data.models

import com.google.gson.annotations.SerializedName

abstract class WidgetUserInteractionBase(
    @field:SerializedName("id")
    open val id: String,
    @field:SerializedName("created_at")
    open val createdAt: String,
    @field:SerializedName("url")
    open val url: String,
    @field:SerializedName("widget_id")
    open val widgetId: String,
    @field:SerializedName("widget_kind")
    open val widgetKind: String
) {

    companion object {
        internal fun <T : WidgetUserInteractionBase> getWidgetClass(widgetKind: String): Class<T> {
            return (if (widgetKind == "emoji-slider") {
                EmojiSliderUserInteraction::class.java
            } else if (widgetKind.contains("quiz") || widgetKind.contains("prediction")) {
                WidgetWithChoicesUserInteraction::class.java
            } else if (widgetKind.contains("poll")) {
                WidgetWithOptionUserInteraction::class.java
            } else {
                CheerMeterUserInteraction::class.java
            }) as Class<T>
        }
    }
}


class EmojiSliderUserInteraction(
    val magnitude: Double,
    id: String,
    createdAt: String,
    url: String,
    widgetId: String,
    widgetKind: String
) : WidgetUserInteractionBase(id, createdAt, url, widgetId, widgetKind)


class WidgetWithChoicesUserInteraction(
    @field:SerializedName("choice_id")
    val choiceId: String,
    id: String,
    createdAt: String,
    url: String,
    widgetId: String,
    widgetKind: String
) : WidgetUserInteractionBase(id, createdAt, url, widgetId, widgetKind)

class WidgetWithOptionUserInteraction(
    @field:SerializedName("option_id")
    val optionId: String,
    id: String,
    createdAt: String,
    url: String,
    widgetId: String,
    widgetKind: String
) : WidgetUserInteractionBase(id, createdAt, url, widgetId, widgetKind)

class CheerMeterUserInteraction(
    @field:SerializedName("vote_count")
    var totalScore: Int,
    id: String,
    createdAt: String,
    url: String,
    widgetId: String,
    widgetKind: String
) : WidgetUserInteractionBase(id, createdAt, url, widgetId, widgetKind)

enum class WidgetKind(val event: String) {
    CHEER_METER("cheer-meter"),
    PREDICTION("prediction"),
    QUIZ("quiz"),
    POLL("poll"),
    IMAGE_SLIDER("emoji-slider");

    companion object {
        private val map = values().associateBy(WidgetKind::event)
        fun fromString(type: String) = map[type]
    }

    fun getType(): String {
        return event
    }
}