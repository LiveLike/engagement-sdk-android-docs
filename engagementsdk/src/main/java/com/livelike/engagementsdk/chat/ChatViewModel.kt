package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.CHAT_PROVIDER
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.chat.chatreaction.ChatReactionRepository
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.services.network.ChatDataClient
import com.livelike.engagementsdk.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.getBlockedUsers
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import kotlinx.coroutines.launch
import java.util.Random

internal class ChatViewModel(
    val analyticsService: AnalyticsService,
    val userStream: Stream<LiveLikeUser>,
    val programRepository: ProgramRepository,
    val animationEventsStream: SubscriptionManager<ViewAnimationEvents>,
    val stickerPackRepository: StickerPackRepository
) : ChatRenderer, ViewModel() {

    var chatListener: ChatEventListener? = null
    var chatAdapter: ChatRecyclerAdapter = ChatRecyclerAdapter(analyticsService, ::reportChatMessage, stickerPackRepository, ChatReactionRepository(programRepository.programId))
    val messageList = mutableListOf<ChatMessage>()
    internal val eventStream: Stream<String> = SubscriptionManager(true)
    var currentChatRoom: ChatRoom? = null
    set(value) {
        field = value
        chatAdapter.isPublicChat = currentChatRoom?.id == programRepository.program.defaultChatRoom?.id
    }
    internal var chatLoaded = false
        set(value) {
            field = value
            if (field) {
                eventStream.onNext(EVENT_LOADING_COMPLETE)
            } else {
                eventStream.onNext(EVENT_LOADING_STARTED)
            }
        }
    private val dataClient: ChatDataClient = EngagementDataClientImpl()

    companion object {
        const val EVENT_NEW_MESSAGE = "new-message"
        const val EVENT_MESSAGE_DELETED = "deletion"
        const val EVENT_MESSAGE_ID_UPDATED = "id-updated"
        const val EVENT_LOADING_COMPLETE = "loading-complete"
        const val EVENT_LOADING_STARTED = "loading-started"
    }

    override fun displayChatMessage(message: ChatMessage) {
        if (message.channel != currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER)) return
        if (getBlockedUsers().contains(message.senderId)) {
            return
        }
        messageList.add(message.apply {
            isFromMe = userStream.latest()?.id == senderId
            reactionsList = chatAdapter.chatReactionRepository.reactionList
        })
        uiScope.launch {
            chatAdapter.submitList(ArrayList(messageList))
            eventStream.onNext(EVENT_NEW_MESSAGE)
        }
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
            chatAdapter.submitList(ArrayList(messageList))
            chatAdapter.notifyDataSetChanged()
        }
    }

    private fun reportChatMessage(message: ChatMessage) {
        uiScope.launch {
            dataClient.reportMessage(programRepository.program.id, message, userStream.latest()?.accessToken)
        }
    }

    fun flushMessages() {
        messageList.clear()
        chatAdapter.submitList(messageList)
    }
}
