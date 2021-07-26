package com.livelike.livelikedemo.models

import com.livelike.livelikedemo.CreatedBy
import com.livelike.livelikedemo.Option

data class CheerMeterRequestResponse(
    val cheer_type: String,
    val options: List<Option>,
    val program_date_time: String? = null,
    val program_id: String,
    val question: String,
    val timeout: String,
    val created_at: String? = null,
    val created_by: CreatedBy? = null,
    val custom_data: Any? = null,
    val engagement_count: Int? = null,
    val engagement_percent: String? = null,
    val id: String? = null,
    val impression_count: Int? = null,
    val impression_url: String? = null,
    val interaction_url: String? = null,
    val kind: String? = null,
    val publish_delay: String? = null,
    val schedule_url: String? = null,
    val status: String? = null,
    val subscribe_channel: String? = null,
    val translatable_fields: List<String>? = null,
    val unique_impression_count: Int? = null,
    val url: String? = null
)
