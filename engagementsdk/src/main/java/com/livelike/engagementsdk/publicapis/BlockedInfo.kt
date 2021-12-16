package com.livelike.engagementsdk.publicapis

import com.google.gson.annotations.SerializedName

data class BlockedInfo(
    val id: String,
    val url: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("blocked_profile")
    val blockedProfile: LiveLikeUserApi,
    @SerializedName("blocked_by_profile")
    val blockedByProfile: LiveLikeUserApi,
    @SerializedName("blocked_profile_id")
    val blockedProfileID: String,
    @SerializedName("blocked_by_profile_id")
    val blockedByProfileId: String
)

internal data class BlockedProfileListResponse(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<BlockedInfo>
)