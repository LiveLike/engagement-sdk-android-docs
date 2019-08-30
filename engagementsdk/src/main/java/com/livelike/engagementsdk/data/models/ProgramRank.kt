package com.livelike.engagementsdk.data.models

import com.google.gson.annotations.SerializedName

data class ProgramRank(
    @SerializedName("id")
    val id: String,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("points")
    val points: Int,
    @SerializedName("points_to_next_badge")
    val pointsToNextBadge: Any,
    @SerializedName("previous_badge")
    val previousBadge: Any,
    @SerializedName("next_badge")
    val nextBadge: Any,
    @SerializedName("current_badge")
    val currentBadge: Any,
    @SerializedName("rank")
    val rank: Int,
    @SerializedName("total_players")
    val totalPlayers: Int,
    @SerializedName("total_points")
    val totalPoints: Int
)
