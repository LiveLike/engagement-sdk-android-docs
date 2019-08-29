package com.livelike.engagementsdk.services.network

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser

internal interface DataClient {
    fun getProgramData(url: String, responseCallback: (program: Program?) -> Unit)
    fun getUserData(clientId: String, accessToken: String, responseCallback: (livelikeUser: LiveLikeUser?) -> Unit)
    fun createUserData(clientId: String, responseCallback: (livelikeUser: LiveLikeUser) -> Unit)
    suspend fun patchUser(clientId: String, userJson: JsonObject, accessToken: String?)
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
    val chatChannel: String,
    @SerializedName("analytics_properties")
    val analyticsProps: Map<String, String>,
    @SerializedName("rewards_type")
    val rewardsType: String // none, points, bagdes
)

internal interface EngagementSdkDataClient {
    fun getEngagementSdkConfig(url: String, responseCallback: (config: EngagementSDK.SdkConfiguration) -> Unit)
}
