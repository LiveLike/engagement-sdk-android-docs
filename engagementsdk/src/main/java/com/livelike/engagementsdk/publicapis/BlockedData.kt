package com.livelike.engagementsdk.publicapis

import com.google.gson.annotations.SerializedName

enum class BlockType(val key: String) {
    BLOCK_EVERYWHERE("block-everywhere")
}

data class BlockedData(
    val id: String,
    val url: String,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("block_type")
    val blockType: String,

    @SerializedName("blocked_profile")
    val blockedProfile: LiveLikeUserApi,

    @SerializedName("blocked_profile_id")
    val blockedProfileID: String
)

internal data class BlockedProfileListResponse(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<BlockedData>
)