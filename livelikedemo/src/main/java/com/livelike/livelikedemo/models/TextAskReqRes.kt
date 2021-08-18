package com.livelike.livelikedemo.models

import com.livelike.livelikedemo.CreatedBy

data class TextAskRequest(

    val prompt: String? = null,
    val program_date_time: String? = null,
    val program_id: String,
    val timeout: String,
    val title: String? = null
)

data class TextAskResponse(
    val created_at: String,
    val created_by: CreatedBy,
    val custom_data: Any,
    val engagement_count: Any,
    val engagement_percent: Any,
    val id: String,
    val image_url: String,
    val impression_count: Int,
    val impression_url: String,
    val interaction_url: String,
    val kind: String,
    val link_label: String,
    val link_url: String,
    val program_date_time: String,
    val program_id: String,
    val publish_delay: String,
    val published_at: Any,
    val reactions: List<Any>,
    val rewards_url: Any,
    val schedule_url: String,
    val scheduled_at: Any,
    val status: String,
    val subscribe_channel: String,
    val text: String,
    val timeout: String,
    val title: String,
    val prompt: String,
    val translatable_fields: List<String>,
    val unique_impression_count: Int,
    val url: String,
    val reply_url: String,
    val confirmation_message:String
)