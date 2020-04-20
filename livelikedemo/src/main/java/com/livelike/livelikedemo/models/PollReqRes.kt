package com.livelike.livelikedemo.models

import com.livelike.livelikedemo.CreatedBy
import com.livelike.livelikedemo.Option

data class PollRequest(
    val options: List<Option>,
    val program_date_time: String? = null,
    val program_id: String,
    val question: String,
    val timeout: String
)

data class PollResponse(
    val created_at: String,
    val created_by: CreatedBy,
    val custom_data: Any,
    val engagement_count: Int,
    val engagement_percent: String,
    val id: String,
    val impression_count: Int,
    val impression_url: String,
    val interaction_url: String,
    val kind: String,
    val options: List<Option>,
    val program_date_time: String,
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
    val translatable_fields: List<String>,
    val unique_impression_count: Int,
    val url: String
)