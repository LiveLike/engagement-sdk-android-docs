package com.livelike.livelikedemo.models

import com.google.gson.annotations.SerializedName
import com.livelike.livelikedemo.CreatedBy
import com.livelike.livelikedemo.Option

class NumberPredictionRequest(
    @field:SerializedName("confirmation_message")
    val confirmationMessage: String? = null,
    val options: List<Option>? = null,
    val program_date_time: String? = null,
    @field:SerializedName("program_id")
    val programId: String? = null,
    val question: String? = null,
    val timeout: String? = null
)

data class NumberPredictionResponse(
    val confirmation_message: String? = null,
    @field:SerializedName("created_at")
    val createdAt: String? = null,
    @field:SerializedName("created_by")
    val createdBy: CreatedBy? = null,
    val custom_data: Any? = null,
    val engagement_count: Int? = null,
    val engagement_percent: String? = null,
    @field:SerializedName("follow_up_url")
    val followUpUrl: String? = null,
    val follow_ups: List<FollowUp>? = null,
    val id: String? = null,
    val impression_count: Int? = null,
    val impression_url: String? = null,
    @field:SerializedName("interaction_url")
    val interactionUrl: String? = null,
    val kind: String? = null,
    val options: ArrayList<OptionX>? = null,
    @field:SerializedName("program_date_time")
    val programDateTime: String? = null,
    @field:SerializedName("program_id")
    val programId: String? = null,
    val publish_delay: String? = null,
    @field:SerializedName("published_at")
    val publishedAt: Any? = null,
    val question: String? = null,
    val reactions: List<Any>? = null,
    val rewards_url: Any? = null,
    @field:SerializedName("schedule_url")
    val scheduleUrl: String? = null,
    @field:SerializedName("scheduled_at")
    val scheduledAt: Any? = null,
    val status: String? = null,
    @field:SerializedName("subscribe_channel")
    val subscribeChannel: String? = null,
    val timeout: String? = null,
    @field:SerializedName("translatable_fields")
    val translatable_fields: List<String>? = null,
    val unique_impression_count: Int? = null,
    val url: String? = null,
    @field:SerializedName("average_magnitude")
    val averageMagnitude: String? = null,
    @field:SerializedName("initial_magnitude")
    val initialMagnitude: String? = null,
    val vote_url: String? = null
)
