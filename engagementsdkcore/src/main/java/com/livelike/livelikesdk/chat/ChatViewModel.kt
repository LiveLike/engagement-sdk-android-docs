package com.livelike.livelikesdk.chat

import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.livelikesdk.data.repository.UserRepository
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.utils.debounce

class ChatViewModel(val analyticsService: AnalyticsService) : ChatRenderer {
    var chatListener: ChatEventListener? = null
    var chatAdapter: ChatRecyclerAdapter = ChatRecyclerAdapter(analyticsService)
    private val messageList = mutableListOf<ChatMessage>()
    private val eventStream: SubscriptionManager<String> = SubscriptionManager()
    internal val debouncedStream = eventStream.debounce(1000)

    companion object {
        const val EVENT_NEW_MESSAGE = "new-message"
        const val EVENT_MESSAGE_DELETED = "deletion"
        const val EVENT_MESSAGE_ID_UPDATED = "id-updated"
        const val EVENT_LOADING_COMPLETE = "loading-complete"
    }

    override fun displayChatMessage(message: ChatMessage) {
        messageList.add(message.apply {
            isFromMe = UserRepository.currentUserStream.latest()?.sessionId == senderId
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
        eventStream.onNext(EVENT_LOADING_COMPLETE)
    }
}
