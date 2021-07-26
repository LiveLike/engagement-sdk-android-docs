package com.livelike.livelikedemo.models

import com.livelike.livelikedemo.CreatedBy
import com.livelike.livelikedemo.Option

data class FollowUpResponse(
    val correct_option_id: String,
    val created_at: String,
    val created_by: CreatedBy,
    val custom_data: Any,
    val engagement_count: Any,
    val engagement_percent: Any,
    val id: String,
    val image_prediction_id: String,
    val image_prediction_url: String,
    val impression_count: Int,
    val impression_url: String,
    val interaction_url: String,
    val kind: String,
    val options: List<Option>,
    val program_date_time: String,
    val program_id: String,
    val publish_delay: String,
    val published_at: String,
    val question: String,
    val reactions: List<Any>,
    val rewards_url: Any,
    val schedule_url: String,
    val scheduled_at: String,
    val status: String,
    val subscribe_channel: String,
    val timeout: String,
    val translatable_fields: List<Any>,
    val unique_impression_count: Int,
    val url: String
)

data class FollowUpRequest(
    val options: List<OptionX>?,
    val program_date_time: String?,
    val question: String?,
    val scheduled_at: Any?,
    val timeout: String?
)

data class FollowUp(
    val correct_option_id: Any,
    val created_at: String,
    val custom_data: Any,
    val engagement_count: Any,
    val engagement_percent: Any,
    val id: String,
    val image_prediction_id: String,
    val image_prediction_url: String,
    val impression_count: Int,
    val impression_url: String,
    val interaction_url: String,
    val kind: String,
    val options: List<Option>,
    val program_date_time: Any,
    val program_id: String,
    val publish_delay: String,
    val published_at: Any,
    val question: String,
    val reactions: List<Any>,
    val rewards_url: Any,
    val schedule_url: String,
    val scheduled_at: Any,
    val status: String,
    val subscribe_channel: String,
    val timeout: String,
    val translatable_fields: List<Any>,
    val unique_impression_count: Int,
    val url: String
)

data class OptionX(
    val description: String,
    val id: String,
    val image_url: String,
    var is_correct: Boolean,
    val translatable_fields: List<String>,
    val url: String,
    var vote_count: Int,
    val vote_url: String
)
