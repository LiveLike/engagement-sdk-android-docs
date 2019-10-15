package com.livelike.engagementsdk.services.network

import com.google.gson.JsonObject
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.data.models.Program
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile

internal interface DataClient {
    fun getProgramData(url: String, responseCallback: (program: Program?) -> Unit)
    fun getUserData(clientId: String, accessToken: String, responseCallback: (livelikeUser: LiveLikeUser?) -> Unit)
    fun createUserData(clientId: String, responseCallback: (livelikeUser: LiveLikeUser) -> Unit)
    suspend fun patchUser(clientId: String, userJson: JsonObject, accessToken: String?)
}

internal interface EngagementSdkDataClient {
    fun getEngagementSdkConfig(url: String, responseCallback: (config: EngagementSDK.SdkConfiguration) -> Unit)
}

internal interface WidgetDataClient {
    suspend fun voteAsync(widgetVotingUrl: String, voteId: String, accessToken: String?)
    suspend fun voteAsync(widgetVotingUrl: String, voteCount: Int, accessToken: String?,isUpdate:Boolean):String
    fun registerImpression(impressionUrl: String)
    suspend fun rewardAsync(rewardUrl: String, analyticsService: AnalyticsService, accessToken: String?): ProgramGamificationProfile?
}

internal interface ChatDataClient {
    suspend fun reportMessage(programId: String, message: ChatMessage, accessToken: String?)
}