package com.livelike.engagementsdk.gamification.models

import android.animation.TimeInterpolator
import com.google.gson.annotations.SerializedName
import java.sql.Time
import java.time.LocalDateTime
import java.util.Date

data class ProfileBadge(
    @SerializedName("awarded_at")
    val awardedAt: String,
    @SerializedName("badge")
    val badge: Badge
)