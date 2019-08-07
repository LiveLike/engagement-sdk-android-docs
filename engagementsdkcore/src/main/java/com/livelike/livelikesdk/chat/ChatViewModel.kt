package com.livelike.livelikesdk.chat

import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.ChatAdapter

internal class ChatViewModel(val analyticService: AnalyticsService) {
    fun clear() {
        chatAdapter = null
        chatLastPos = null
    }

    var chatAdapter: ChatAdapter? = null
    var chatLastPos: Int? = null
}
