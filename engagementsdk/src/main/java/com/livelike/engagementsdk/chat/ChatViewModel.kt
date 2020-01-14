package com.livelike.engagementsdk.chat

import android.util.Log
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
import com.livelike.engagementsdk.utils.logError
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.reflect.KFunction0

internal class ChatViewModel(
    val analyticsService: AnalyticsService,
    val userStream: Stream<LiveLikeUser>,
    val programRepository: ProgramRepository,
    val animationEventsStream: SubscriptionManager<ViewAnimationEvents>
) : ChatRenderer, ViewModel() {

    var chatListener: ChatEventListener? = null
    var chatAdapter: ChatRecyclerAdapter = ChatRecyclerAdapter(analyticsService, ::reportChatMessage)
    val messageList = mutableListOf<ChatMessage>()
    internal val eventStream: Stream<String> = SubscriptionManager(true)
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
    }

    override fun displayChatMessage(message: ChatMessage) {
        if (message.channel != currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER)) return
        if (getBlockedUsers().contains(message.senderId)) {
            return
        }
        messageList.add(message.apply {
            isFromMe = userStream.latest()?.id == senderId
        })
        uiScope.launch{
            doSortAndSubmitMessageList()
        }
    }

    private fun debounce(
        waitMs: Long = 300L,
        coroutineScope: CoroutineScope,
        destinationFunction: KFunction0<Unit>
    ): () -> Unit {
        var debounceJob: Job? = null
        return {
            debounceJob?.cancel()
            debounceJob = coroutineScope.launch {
                delay(waitMs)
                destinationFunction()
            }
        }
    }

    // Debouce the sorting operation to avoid the heavy call being done too often
    val doSortAndSubmitMessageList: ()->Unit = debounce(300L, GlobalScope, ::sortAndSubmitMessageList)

    private fun sortAndSubmitMessageList() = runBlocking(Dispatchers.Default){
        Log.d("Sorting", "Sorting")
        messageList.sortBy {
            if(it.timeStamp.isNullOrEmpty() || it.timeStamp == "0") {
                // If no timestamp, sort by timetoken
                it.timetoken
            }
            else {
                // else, sort by timestamp
                it.timeStamp.toLongOrNull() ?: 0}
            }
        withContext(Dispatchers.Main){
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

    override fun updateChatMessageTimeToken(messageId: String, timetoken: String) {
        messageList.find {
            it.id == messageId
        }?.apply {
            this.timetoken = timetoken.toLong()
        }

        uiScope.launch{
            doSortAndSubmitMessageList()
        }
    }

    override fun loadingCompleted() {
        if (!chatLoaded) {
            chatLoaded = true
            chatAdapter.submitList(ArrayList(messageList))
            chatAdapter.notifyDataSetChanged()
        }else{
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

    fun loadPreviousMessages(){
        currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER)?.let {channel ->
            if (chatRepository != null) {
                chatRepository?.loadPreviousMessages(
                    channel,
                    messageList.first().timetoken
                )
            }else{
                logError { "Chat repo is null" }
            }
        }
    }

}
