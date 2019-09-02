package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.utils.SubscriptionManager

internal class ChatViewModel(
    val analyticsService: AnalyticsService,
    val userStream: Stream<LiveLikeUser>,
    val programRepository: ProgramRepository
) : ChatRenderer {
    var chatListener: ChatEventListener? = null
    var chatAdapter: ChatRecyclerAdapter = ChatRecyclerAdapter(analyticsService)
    private val messageList = mutableListOf<ChatMessage>()
    internal val eventStream: Stream<String> = SubscriptionManager(false)
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
        chatAdapter.notifyDataSetChanged()
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
