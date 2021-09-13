package com.livelike.engagementsdk.core.data.models

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.LiveLikeUser

data class UserSearchApiResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("next") val next: String,
    @SerializedName("previous") val previous: String,
    @SerializedName("results") val results: List<LiveLikeUser>
)