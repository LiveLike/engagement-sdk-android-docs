package com.livelike.engagementsdk.chat.data.remote

internal data class PubnubChatListResponse(
    var results: List<PubnubChatMessage>
)

internal data class PubnubChatListCountResponse(val count: Int)