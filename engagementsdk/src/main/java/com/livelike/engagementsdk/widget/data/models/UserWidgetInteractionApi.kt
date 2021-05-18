package com.livelike.engagementsdk.widget.data.models

import com.google.gson.annotations.SerializedName


/**
* User interaction Api response model class
**/

internal data class UserWidgetInteractionApi(
    val interactions: Interactions
)

internal data class Interactions(
    @field:SerializedName("cheer-meter")
    val cheerMeter: List<CheerMeterUserInteraction>?,
    @field:SerializedName("emoji-slider")
    val emojiSlider: List<EmojiSliderUserInteraction>?,
    @field:SerializedName("text-poll")
    val textPoll: List<WidgetWithOptionUserInteraction>?,
    @field:SerializedName("image-poll")
    val imagePoll: List<WidgetWithOptionUserInteraction>?,
    @field:SerializedName("text-quiz")
    val textQuiz: List<WidgetWithChoicesUserInteraction>?,
    @field:SerializedName("image-quiz")
    val imageQuiz: List<WidgetWithChoicesUserInteraction>?,
    @field:SerializedName("text-prediction")
    val textPrediction: List<WidgetWithOptionUserInteraction>?,
    @field:SerializedName("image-prediction")
    val imagePrediction: List<WidgetWithOptionUserInteraction>?,

)
