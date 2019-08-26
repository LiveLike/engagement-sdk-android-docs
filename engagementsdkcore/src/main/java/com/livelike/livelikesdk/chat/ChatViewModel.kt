package com.livelike.livelikesdk.chat

import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.livelikesdk.Stream
import com.livelike.livelikesdk.utils.SubscriptionManager

class ChatViewModel(val analyticsService: AnalyticsService, val userStream: Stream<LiveLikeUser>) : ChatRenderer {
    var chatListener: ChatEventListener? = null
    var chatAdapter: ChatRecyclerAdapter = ChatRecyclerAdapter(analyticsService)
    private val messageList = mutableListOf<ChatMessage>()
    internal val eventStream: SubscriptionManager<String> = SubscriptionManager(false)
    private var chatLoaded = false

    companion object {
        const val EVENT_NEW_MESSAGE = "new-message"
        const val EVENT_MESSAGE_DELETED = "deletion"
        const val EVENT_MESSAGE_ID_UPDATED = "id-updated"
        const val EVENT_LOADING_COMPLETE = "loading-complete"
    }

    override fun displayChatMessage(message: ChatMessage) {
        messageList.add(message.apply {
            isFromMe = userStream.latest()?.id == senderId
        })
        chatAdapter.submitList(ArrayList(messageList))
        eventStream.onNext(EVENT_NEW_MESSAGE)
    }

    override fun deleteChatMessage(messageId: String) {
        messageList.find { it.id == messageId }?.apply {
            message = "Redacted"
        }
        chatAdapter.submitList(ArrayList(messageList))
        eventStream.onNext(EVENT_MESSAGE_DELETED)
    }

    override fun updateChatMessageId(oldId: String, newId: String) {
        messageList.find {
            it.id == oldId
        }?.apply {
            id = newId
        }
    }

    override fun loadingCompleted() {
        if (!chatLoaded) {
            chatLoaded = true
            eventStream.onNext(EVENT_LOADING_COMPLETE)
        }
    }
}
