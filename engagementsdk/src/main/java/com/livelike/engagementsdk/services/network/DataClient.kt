package com.livelike.engagementsdk.services.network

import com.google.gson.JsonObject
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.data.models.Program

internal interface DataClient {
    fun getProgramData(url: String, responseCallback: (program: Program?) -> Unit)
    fun getUserData(clientId: String, accessToken: String, responseCallback: (livelikeUser: LiveLikeUser?) -> Unit)
    fun createUserData(clientId: String, responseCallback: (livelikeUser: LiveLikeUser) -> Unit)
    suspend fun patchUser(clientId: String, userJson: JsonObject, accessToken: String?)
}

internal interface EngagementSdkDataClient {
    fun getEngagementSdkConfig(url: String, responseCallback: (config: EngagementSDK.SdkConfiguration) -> Unit)
}
