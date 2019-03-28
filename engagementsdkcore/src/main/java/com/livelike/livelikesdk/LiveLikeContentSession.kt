package com.livelike.livelikesdk

import com.livelike.engagementsdkapi.LiveLikeUser

internal interface LiveLikeDataClient {
    fun getLiveLikeProgramData(url: String, responseCallback: (program: Program) -> Unit)
    fun getLiveLikeUserData(url: String, responseCallback: (livelikeUser: LiveLikeUser) -> Unit)
}


internal data class Program(
    val programUrl: String,
    val timelineUrl: String,
    val clientId: String,
    val id: String,
    val title: String,
    val widgetsEnabled: Boolean,
    val chatEnabled: Boolean,
    val subscribeChannel: String,
    val chatChannel: String,
    val streamUrl: String
)
