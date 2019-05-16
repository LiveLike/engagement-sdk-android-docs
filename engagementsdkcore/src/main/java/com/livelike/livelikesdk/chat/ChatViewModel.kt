package com.livelike.livelikesdk.chat

import android.arch.lifecycle.ViewModel
import com.livelike.engagementsdkapi.ChatAdapter
import com.livelike.livelikesdk.LiveLikeSDK.Companion.currentSession

class ChatViewModel : ViewModel() {
    private var chatAdapters: MutableMap<String, ChatAdapter?> = mutableMapOf()
    fun currentChatAdapter(): ChatAdapter? {
        return chatAdapters[getProgramId()]
    }

    fun addAdapter(chatAdapter: ChatAdapter) {
        chatAdapters[getProgramId()] = chatAdapter
    }

    private fun getProgramId(): String {
        return try {
            currentSession.programUrl
        } catch (e: Exception) {
            ""
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}