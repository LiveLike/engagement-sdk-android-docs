package com.livelike.livelikesdk

import com.livelike.engagementsdkapi.LiveLikeUser
import com.google.gson.annotations.SerializedName;


internal interface LiveLikeDataClient {
    fun getLiveLikeProgramData(url: String, responseCallback: (program: Program?) -> Unit)
    fun getLiveLikeUserData(url: String, responseCallback: (livelikeUser: LiveLikeUser) -> Unit)
}

internal data class Program(
    @SerializedName("url")
    val programUrl: String?,
    @SerializedName("timeline_url")
    val timelineUrl: String,
    @SerializedName("content_id")
    val contentId: String,
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
    val chatChannel: String
)
