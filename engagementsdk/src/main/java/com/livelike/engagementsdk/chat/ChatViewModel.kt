package com.livelike.engagementsdk.chat

import android.content.Context
import android.net.Uri
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.CHAT_PROVIDER
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.chat.chatreaction.ChatReactionRepository
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.services.network.ChatDataClient
import com.livelike.engagementsdk.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.getBlockedUsers
import com.livelike.engagementsdk.utils.logError
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

internal class ChatViewModel(
    val analyticsService: AnalyticsService,
    val userStream: Stream<LiveLikeUser>,
    val programRepository: ProgramRepository,
    val animationEventsStream: SubscriptionManager<ViewAnimationEvents>
) : ChatRenderer, ViewModel() {

    var chatListener: ChatEventListener? = null
    var chatAdapter: ChatRecyclerAdapter = ChatRecyclerAdapter(analyticsService, ::reportChatMessage)
    val messageList = mutableListOf<ChatMessage>()
    internal val eventStream: Stream<String> = SubscriptionManager(true,this.javaClass.simpleName)
    var currentChatRoom: ChatRoom? = null
        set(value) {
            field = value
            chatAdapter.isPublicChat = currentChatRoom?.id == programRepository?.program?.defaultChatRoom?.id
        }

    var stickerPackRepository: StickerPackRepository? = null
        set(value) {
            field = value
            value?.let { chatAdapter.stickerPackRepository = value }
        }
    val stickerPackRepositoryFlow = flow {
        while (stickerPackRepository == null) {
            delay(1000)
        }
        emit(stickerPackRepository!!)
    }
    var chatReactionRepository: ChatReactionRepository? = null
        set(value) {
            field = value
            value?.let {
                chatAdapter.chatReactionRepository = value }
        }
    var chatRepository: ChatRepository? = null
        set(value) {
            field = value
            chatAdapter.chatRepository = value
        }
    var reportUrl: String? = null

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
        const val EVENT_MESSAGE_TIMETOKEN_UPDATED = "id-updated"
        const val EVENT_LOADING_COMPLETE = "loading-complete"
        const val EVENT_LOADING_STARTED = "loading-started"
        const val EVENT_REACTION_ADDED = "reaction-added"
        const val EVENT_REACTION_REMOVED = "reaction-removed"
    }

    override fun displayChatMessage(message: ChatMessage) {
        if (message.channel != currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER)) return
        if (getBlockedUsers().contains(message.senderId)) {
            return
        }

        val imageUrl = message.imageUrl

        if(message.messageEvent == PubnubChatEventType.IMAGE_CREATED && !imageUrl.isNullOrEmpty()){
            message.message = CHAT_MESSAGE_IMAGE_TEMPLATE.replace("message", imageUrl)
        }
        if (messageList.size == 0) {
            messageList.add(message.apply {
                isFromMe = userStream.latest()?.id == senderId
            })
        } else {
            messageList.first().let {
                if (message.timetoken != 0L && it.timetoken > message.timetoken) {
                    messageList.add(0, message.apply {
                        isFromMe = userStream.latest()?.id == senderId
                    })
                } else {
                    messageList.add(message.apply {
                        isFromMe = userStream.latest()?.id == senderId
                    })
                }
            }
        }

        uiScope.launch {
            chatAdapter.submitList(ArrayList(messageList.toSet()))
            eventStream.onNext(EVENT_NEW_MESSAGE)
        }
    }

    override fun removeMessageReaction(messagePubnubToken: Long, emojiId: String) {
        messageList.forEach { chatMessage ->
            chatMessage.apply {
                if (this.timetoken == messagePubnubToken) {
                    emojiCountMap[emojiId] = (emojiCountMap[emojiId] ?: 0) - 1
                    uiScope.launch { chatAdapter.notifyDataSetChanged() }
                    return@forEach
                }
                // remember case not handled for now if same user removes its reaction while using 2 devices
            }
        }
    }

    override fun addMessageReaction(
        isOwnReaction: Boolean,
        messagePubnubToken: Long,
        chatMessageReaction: ChatMessageReaction
    ) {

        messageList.forEach { chatMessage ->
            chatMessage.apply {
                if (this.timetoken == messagePubnubToken) {
                    if (isOwnReaction) {
                        if (chatMessage?.myChatMessageReaction?.emojiId == chatMessageReaction.emojiId) {
                            chatMessage?.myChatMessageReaction?.pubnubActionToken = chatMessageReaction.pubnubActionToken
                        }
                    } else {
                        val emojiId = chatMessageReaction.emojiId
                        emojiCountMap[emojiId] = (emojiCountMap[emojiId] ?: 0) + 1
                        uiScope.launch { chatAdapter.notifyDataSetChanged() }
                    }
                    return@forEach
                }
            }
        }
    }

    override fun deleteChatMessage(messageId: String) {
        messageList.find { it.id == messageId }?.apply {
            message = "Redacted"
        }
        chatAdapter.submitList(ArrayList(messageList.toSet()))
        eventStream.onNext(EVENT_MESSAGE_DELETED)
    }

    override fun updateChatMessageTimeToken(messageId: String, timetoken: String) {
        messageList.find {
            it.id == messageId
        }?.apply {
            this.timetoken = timetoken.toLong()
        }
    }

    override fun loadingCompleted() {
        if (!chatLoaded) {
            chatAdapter.submitList(ArrayList(messageList.toSet()))
            chatAdapter.notifyDataSetChanged()
            chatLoaded = true
        } else {
            eventStream.onNext(EVENT_LOADING_COMPLETE)
        }
    }

    private fun reportChatMessage(message: ChatMessage) {
        uiScope.launch {
            reportUrl?.let { reportUrl -> dataClient.reportMessage(reportUrl, message, userStream.latest()?.accessToken) }
        }
    }

    fun flushMessages() {
        messageList.clear()
        chatAdapter.submitList(messageList)
    }

    fun loadPreviousMessages() {
        currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER)?.let { channel ->
            if (chatRepository != null) {
                chatRepository?.loadPreviousMessages(
                    channel,
                    messageList.first().timetoken
                )
            } else {
                logError { "Chat repo is null" }
            }
        }
    }

    fun uploadAndPostImage(context : Context, chatMessage: ChatMessage, timedata: EpochTime) {
        GlobalScope.launch (Dispatchers.IO) {
            val url = Uri.parse(chatMessage.message.substring(1, chatMessage.message.length-1))
            val fileBytes = context.contentResolver.openInputStream(url)?.readBytes()
            val imageUrl = dataClient.uploadImage(currentChatRoom!!.uploadUrl, userStream.latest()!!.accessToken, fileBytes!!)
            chatMessage.messageEvent = PubnubChatEventType.IMAGE_CREATED
            chatMessage.imageUrl = imageUrl
            chatListener?.onChatMessageSend(chatMessage, timedata)
        }
    }
}
