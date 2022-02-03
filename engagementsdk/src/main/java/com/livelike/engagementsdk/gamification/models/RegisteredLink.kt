package com.livelike.engagementsdk.gamification.models

import com.google.gson.annotations.SerializedName

data class RegisteredLink (
    @field:SerializedName("id")
    val id: String,
    @field:SerializedName("name")
    val name: String,
    @field:SerializedName("description")
    val description: String?,
    @field:SerializedName("url")
    val url : String,
)
