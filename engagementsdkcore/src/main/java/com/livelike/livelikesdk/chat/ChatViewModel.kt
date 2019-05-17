package com.livelike.livelikesdk.chat

import android.arch.lifecycle.ViewModel
import com.livelike.engagementsdkapi.ChatAdapter
import com.livelike.livelikesdk.LiveLikeSDK.Companion.currentSession

class ChatViewModel : ViewModel() {
    private var chatAdapters: MutableMap<String, ChatAdapter?> = mutableMapOf()
    private var chatLastPos: MutableMap<String, Int?> = mutableMapOf()
    fun currentChatAdapter(): ChatAdapter? {
        return chatAdapters[getProgramId()]
    }

    fun addAdapter(chatAdapter: ChatAdapter) {
        chatAdapters[getProgramId()] = chatAdapter
    }

    fun currentLastPos(): Int? {
        return chatLastPos[getProgramId()]
    }

    fun setLastPos(lastPos: Int?) {
        chatLastPos[getProgramId()] = lastPos
    }

    private fun getProgramId(): String {
        return try {
            currentSession.programUrl
        } catch (e: Exception) {
            ""
        }
    }
}