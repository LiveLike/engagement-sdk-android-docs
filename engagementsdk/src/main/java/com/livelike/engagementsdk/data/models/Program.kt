package com.livelike.engagementsdk.data.models

import com.google.gson.annotations.SerializedName

internal data class Program(
    @SerializedName("url")
    val programUrl: String?,
    @SerializedName("timeline_url")
    val timelineUrl: String,
    @SerializedName("rank_url")
    val rankUrl: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("widgets_enabled")
    val widgetsEnabled: Boolean,
    @SerializedName("chat_enabled")
    val chatEnabled: Boolean,
    @SerializedName("subscribe_channel")
    val subscribeChannel: String,
    @SerializedName("sendbird_channel")
    val chatChannel: String,
    @SerializedName("analytics_properties")
    val analyticsProps: Map<String, String>,
    @SerializedName("rewards_type")
    val rewardsType: String // none, points, bagdes
)

enum class RewardsType(val key: String) {
    NONE("none"),
    POINTS("points"),
    BADGES("badges");
}
