package com.livelike.engagementsdk.widget.data.models

abstract class WidgetUserInteractionBase(
    open val id: String,
    open val created_at: String,
    open val url: String,
    open val widget_id: String,
    open val widget_kind: String
)


data class EmojiSliderUserInteraction(
   val magnitude: Double,
   override val id: String,
   override val created_at: String,
   override val url: String,
   override val widget_id: String,
   override val widget_kind: String
) : WidgetUserInteractionBase(id, created_at, url, widget_id, widget_kind)


data class WidgetWithChoicesUserInteraction(
   val choice_id: String,
   override val id: String,
   override val created_at: String,
   override val url: String,
   override val widget_id: String,
   override val widget_kind: String
) : WidgetUserInteractionBase(id, created_at, url, widget_id, widget_kind)


data class WidgetWithOptionUserInteraction(
   val choice_id: String,
   override val id: String,
   override val created_at: String,
   override val url: String,
   override val widget_id: String,
   override val widget_kind: String
) : WidgetUserInteractionBase(id, created_at, url, widget_id, widget_kind)