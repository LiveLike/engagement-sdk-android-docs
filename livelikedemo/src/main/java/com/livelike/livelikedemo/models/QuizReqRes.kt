package com.livelike.livelikedemo.models

import com.livelike.livelikedemo.Choice
import com.livelike.livelikedemo.CreatedBy

data class QuizRequest(
    val choices: List<Choice>? = null,
    val program_date_time: String? = null,
    val program_id: String? = null,
    val question: String? = null,
    val timeout: String? = null
)

data class QuizResponse(
    val choices: List<Choice>?=null,
    val created_at: String?=null,
    val created_by: CreatedBy?=null,
    val custom_data: Any?=null,
    val engagement_count: Int?=null,
    val engagement_percent: String?=null,
    val id: String?=null,
    val impression_count: Int?=null,
    val impression_url: String?=null,
    val interaction_url: String?=null,
    val kind: String?=null,
    val program_date_time: String?=null,
    val program_id: String?=null,
    val publish_delay: String?=null,
    val published_at: Any?=null,
    val question: String?=null,
    val reactions: List<Any>?=null,
    val rewards_url: Any?=null,
    val schedule_url: String?=null,
    val scheduled_at: Any?=null,
    val status: String?=null,
    val subscribe_channel: String?=null,
    val timeout: String?=null,
    val translatable_fields: List<String>?=null,
    val unique_impression_count: Int?=null,
    val url: String?=null
)