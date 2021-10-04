package com.livelike.livelikedemo.models

import com.google.gson.annotations.SerializedName
import com.livelike.livelikedemo.CreatedBy
import com.livelike.livelikedemo.Option

class NumberPredictionRequest(
    @field:SerializedName("confirmation_message")
    val confirmationMessage: String? = null,
    val options: List<Option>? = null,
    @field:SerializedName("program_date_time")
    val programDateTime: String? = null,
    @field:SerializedName("program_id")
    val programId: String? = null,
    val question: String? = null,
    val timeout: String? = null
)

data class NumberPredictionResponse(
    @field:SerializedName("confirmation_message")
    val confirmationMessage: String? = null,
    @field:SerializedName("created_at")
    val createdAt: String? = null,
    @field:SerializedName("created_by")
    val createdBy: CreatedBy? = null,
    val custom_data: Any? = null,
    @field:SerializedName("engagement_count")
    val engagementCount: Int? = null,
    @field:SerializedName("engagement_percent")
    val engagementPercent: String? = null,
    @field:SerializedName("follow_up_url")
    val followUpUrl: String? = null,
    @field:SerializedName("follow_ups")
    val followUps: List<FollowUp>? = null,
    val id: String? = null,
    @field:SerializedName("impression_count")
    val impressionCount: Int? = null,
    @field:SerializedName("impression_url")
    val impressionUrl: String? = null,
    @field:SerializedName("interaction_url")
    val interactionUrl: String? = null,
    val kind: String? = null,
    val options: ArrayList<OptionX>? = null,
    @field:SerializedName("program_date_time")
    val programDateTime: String? = null,
    @field:SerializedName("program_id")
    val programId: String? = null,
    @field:SerializedName("publish_delay")
    val publishDelay: String? = null,
    @field:SerializedName("published_at")
    val publishedAt: Any? = null,
    val question: String? = null,
    val reactions: List<Any>? = null,
    @field:SerializedName("rewards_url")
    val rewardsUrl: Any? = null,
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
    @field:SerializedName("unique_impression_count")
    val uniqueImpressionCount: Int? = null,
    val url: String? = null,
    @field:SerializedName("average_magnitude")
    val averageMagnitude: String? = null,
    @field:SerializedName("initial_magnitude")
    val initialMagnitude: String? = null,
    @field:SerializedName("vote_url")
    val voteUrl: String? = null
)
