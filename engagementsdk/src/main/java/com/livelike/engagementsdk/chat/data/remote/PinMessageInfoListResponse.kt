package com.livelike.engagementsdk.chat.data.remote

import com.example.example.PinMessageInfo
import com.google.gson.annotations.SerializedName

internal data class PinMessageInfoListResponse(
    @field:SerializedName("next")
    val next: String? = null,

    @field:SerializedName("previous")
    val previous: String? = null,

    @field:SerializedName("count")
    val count: Int? = null,

    @field:SerializedName("results")
    val results: List<PinMessageInfo>? = null
)