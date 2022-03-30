package com.livelike.engagementsdk.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.livelike.engagementsdk.*
import com.livelike.engagementsdk.chat.chatreaction.ChatReactionRepository
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.chat.services.network.ChatDataClient
import com.livelike.engagementsdk.chat.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.services.messaging.Error
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.publicapis.BlockedInfo
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.filterList
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import kotlin.collections.set

internal class ChatViewModel(
    private val applicationContext: Context,
    val userStream: Stream<LiveLikeUser>,
    val isPublicRoom: Boolean,
    val animationEventsStream: SubscriptionManager<ViewAnimationEvents>? = null,
    val programRepository: ProgramRepository? = null,
    private val dataClient: ChatDataClient,
    private val errorDelegate: ErrorDelegate? = null
) : ChatRenderer, ViewModel() {

    var chatListener: ChatEventListener? = null
    var analyticsService: AnalyticsService = MockAnalyticsService()
        set(value) {
            field = value
            chatAdapter.analyticsService = value
        }
    internal var showChatAvatarLogo: Boolean = true
    set(value) {
        field = value
        chatAdapter.showChatAvatarLogo = value
    }
    var chatAdapter: ChatRecyclerAdapter =
        ChatRecyclerAdapter(
            analyticsService,
            ::reportChatMessage,
            ::blockProfile
        )
    var messageList = mutableListOf<ChatMessage>()
    private var deletedMessages = hashSetOf<String>()

    var enableQuoteMessage: Boolean = false
        set(value) {
            field = value
            chatAdapter.enableQuoteMessage = value
        }

    internal val eventStream: Stream<String> =
        SubscriptionManager(true)
    var currentChatRoom: ChatRoom? = null
        set(value) {
            field = value
            chatAdapter.chatRoomId = value?.id
            chatAdapter.isPublicChat = isPublicRoom
            chatAdapter.chatRoomName = value?.title
        }

    var liveLikeChatClient: LiveLikeChatClient? = null

    var stickerPackRepository: StickerPackRepository? = null
        set(value) {
            field = value
            value?.let {
                stickerPackRepositoryStream.onNext(value)
            }
            value?.let { chatAdapter.stickerPackRepository = value }
        }
    val stickerPackRepositoryStream: Stream<StickerPackRepository> = SubscriptionManager()
    var chatReactionRepository: ChatReactionRepository? = null
        set(value) {
            field = value
            value?.let {
                chatAdapter.chatReactionRepository = value
            }
        }
    var chatRepository: ChatRepository? = null
        set(value) {
            field = value
            chatAdapter.chatRepository = value
        }
    var reportUrl: String? = null
    var isLastItemVisible = false

    internal var chatLoaded = false
        set(value) {
            field = value
            logDebug { "chatload:$field" }
            if (field) {
                eventStream.onNext(EVENT_LOADING_COMPLETE)
            } else {
                eventStream.onNext(EVENT_LOADING_STARTED)
            }
        }

    override fun displayChatMessages(messages: List<ChatMessage>) {
        Log.d("custom", "messages")
        messages.forEach {
            replaceImageMessageContentWithImageUrl(it)
            it.quoteMessage?.apply {
                replaceImageMessageContentWithImageUrl(this)
            }
        }

        messageList.addAll(
            0,
            messages.filter {
                !deletedMessages.contains(it.id)
            }.filter {
                chatAdapter.chatViewDelegate != null || (chatAdapter.chatViewDelegate == null && it.messageEvent != PubnubChatEventType.CUSTOM_MESSAGE_CREATED)
            }.map {
                it.isFromMe = userStream.latest()?.id == it.senderId
                it.quoteMessage = it.quoteMessage?.apply {
                    message = when (deletedMessages.contains(id)) {
                        true -> applicationContext.getString(R.string.livelike_quote_chat_message_deleted_message)
                        else -> message
                    }
                    isDeleted = deletedMessages.contains(id)
                }
                it
            }
        )

        notifyNewChatMessages()
    }

    override fun displayChatMessage(message: ChatMessage) {
        logDebug {
            "Chat display message: ${message.message} check1:${
                message.channel != currentChatRoom?.channels?.chat?.get(
                    CHAT_PROVIDER
                )
            } check deleted:${deletedMessages.contains(message.id)} has Parent msg: ${message.quoteMessage != null}"
        }
        if (message.channel != currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER)) return

        // if custom message is received, ignore that, custom messages doesn't need to be shown in UI
        // if integrator provide the chatViewDelegate that means we are allowing to show the custom message
        if (chatAdapter.chatViewDelegate == null && message.messageEvent == PubnubChatEventType.CUSTOM_MESSAGE_CREATED) return


        if (deletedMessages.contains(message.id?.lowercase(Locale.getDefault()))) {
            logDebug { "the message is deleted by producer" }
            return
        }

        replaceImageMessageContentWithImageUrl(message)
        messageList.add(
            message.apply {
                isFromMe = userStream.latest()?.id == senderId
                quoteMessage = quoteMessage?.apply {
                    replaceImageMessageContentWithImageUrl(this)
                    this.message = when (deletedMessages.contains(id)) {
                        true -> applicationContext.getString(R.string.livelike_quote_chat_message_deleted_message)
                        else -> this.message
                    }
                    this.isDeleted = deletedMessages.contains(id)
                }
            }
        )

        notifyNewChatMessages()
    }

    private fun notifyNewChatMessages() {
        if (chatLoaded) {
            uiScope.launch {
                chatAdapter.submitList(ArrayList(messageList))
                eventStream.onNext(EVENT_NEW_MESSAGE)
            }
        }
    }

    private fun replaceImageMessageContentWithImageUrl(
        message: ChatMessage
    ) {
        val imageUrl = message.imageUrl
        if (!imageUrl.isNullOrEmpty()) {
            message.message = CHAT_MESSAGE_IMAGE_TEMPLATE.replace("message", imageUrl)
        }
    }

    override fun removeMessageReaction(messagePubnubToken: Long, emojiId: String) {
        messageList.forEachIndexed { index, chatMessage ->
            chatMessage.apply {
                if (this.timetoken == messagePubnubToken) {
                    emojiCountMap[emojiId] = (emojiCountMap[emojiId] ?: 0) - 1
                    uiScope.launch { chatAdapter.notifyItemChanged(index) }
                    return@forEachIndexed
                }
                // remember case not handled for now if same user removes its reaction while using 2 devices
            }
        }
    }

    //TODO: need to check for error message to show in ChatView
    override fun errorSendingMessage(error: Error) {
        if (error.type == MessageError.DENIED_MESSAGE_PUBLISH.name) {
            val index = messageList.indexOfLast { it.clientMessageId == error.clientMessageId }
            if (index > -1) {
                messageList.removeAt(index)
                chatAdapter.submitList(messageList)
                eventStream.onNext(EVENT_MESSAGE_CANNOT_SEND)
            } else {
                logError { "Unable to find the message based on client message id: ${error.clientMessageId}" }
            }
        }
    }

    override fun addMessageReaction(
        isOwnReaction: Boolean,
        messagePubnubToken: Long,
        chatMessageReaction: ChatMessageReaction
    ) {
        logDebug { "add Message Reaction OwnReaction:$isOwnReaction" }
        messageList.forEachIndexed { index, chatMessage ->
            @Suppress("SENSELESS_COMPARISON")
            if (chatMessage != null) { // added null check in reference to ES-1533 (though crash not reproducible at all)
                chatMessage.apply {
                    if (this.timetoken == messagePubnubToken) {
                        if (isOwnReaction) {
                            if (chatMessage.myChatMessageReaction?.emojiId == chatMessageReaction.emojiId) {
                                chatMessage.myChatMessageReaction?.pubnubActionToken =
                                    chatMessageReaction.pubnubActionToken
                                // added notifyItemChange for reaction own ,reference to ES-1734
                                uiScope.launch { chatAdapter.notifyItemChanged(index) }
                            }
                        } else {
                            val emojiId = chatMessageReaction.emojiId
                            emojiCountMap[emojiId] = (emojiCountMap[emojiId] ?: 0) + 1
                            uiScope.launch { chatAdapter.notifyItemChanged(index) }
                        }
                        return@forEachIndexed
                    }
                }
            }
        }
    }

    override fun deleteChatMessage(messageId: String) {
        deletedMessages.add(messageId)
        if (chatLoaded) {
            logDebug { "message is deleted from producer so changing its text" }
            messageList.find {
                it.id?.lowercase(Locale.getDefault()) == messageId
            }?.apply {
                message =
                    applicationContext.getString(R.string.livelike_chat_message_deleted_message)
                isDeleted = true
            }
            messageList.filterList {
                quoteMessage != null && quoteMessage?.id?.lowercase(Locale.getDefault()) == messageId
            }.forEach {
                if (it.quoteMessage != null && it.quoteMessage?.id?.lowercase(Locale.getDefault()) == messageId) {
                    it.apply {
                        quoteMessage = quoteMessage?.apply {
                            message = when (messageId == id) {
                                true -> applicationContext.getString(R.string.livelike_quote_chat_message_deleted_message)
                                else -> message
                            }
                            isDeleted = deletedMessages.contains(id)
                        }
                    }
                }
            }
            uiScope.launch {
                chatAdapter.submitList(ArrayList(messageList.toSet()))
                chatAdapter.currentChatReactionPopUpViewPos = -1
                val index = messageList.indexOfFirst { it.id == messageId }
                val indexList = messageList.getIndexList {
                    it.quoteMessage != null && it.quoteMessage?.id?.lowercase(Locale.getDefault()) == messageId
                }
                notifyIndexUpdate(index)
                indexList.forEach { notifyIndexUpdate(it) }
                eventStream.onNext(EVENT_MESSAGE_DELETED)
            }
        }
    }

    override fun updateChatMessageTimeToken(
        messageId: String,
        clientMessageId: String?,
        timetoken: String,
        createdAt: String?
    ) {
        uiScope.launch {
            messageList.find {
                it.clientMessageId == clientMessageId
            }?.let { cm ->
                cm.timetoken = timetoken.toLong()
                cm.createdAt = createdAt
                cm.id = messageId
                chatAdapter.submitList(ArrayList(messageList))
                chatAdapter.notifyItemChanged(messageList.indexOf(cm))
                eventStream.onNext(EVENT_NEW_MESSAGE)
            }
        }
    }

    private fun notifyIndexUpdate(index: Int) {
        if (index != -1 && index < chatAdapter.itemCount) {
            chatAdapter.notifyItemChanged(index)
        }
    }

    override fun loadingCompleted() {
        logDebug { "Chat loading Completed : $chatLoaded" }
        if (!chatLoaded) {
            chatLoaded = true
            chatAdapter.submitList(ArrayList(messageList.toSet()))
        } else {
            eventStream.onNext(EVENT_LOADING_COMPLETE)
        }
    }

    private fun blockProfile(profileId: String) {
        liveLikeChatClient?.blockProfile(profileId,
            object : LiveLikeCallback<BlockedInfo>() {
                override fun onResponse(result: BlockedInfo?, error: String?) {
                    error?.let {
                        errorDelegate?.onError(it)
                    }
                    result?.let {
                        logDebug { "Block User: ${it.blockedProfileID} ,By User: ${it.blockedByProfileId}" }

                    }
                }
            })
    }

    private fun reportChatMessage(message: ChatMessage) {
        uiScope.launch {
            reportUrl?.let { reportUrl ->
                dataClient.reportMessage(
                    reportUrl,
                    message,
                    userStream.latest()?.accessToken
                )
            }
        }
    }

    internal fun refreshWithDeletedMessage() {
        messageList.removeAll { deletedMessages.contains(it.id?.lowercase(Locale.getDefault())) }
        uiScope.launch {
            chatAdapter.submitList(ArrayList(messageList))
        }
    }

    fun flushMessages() {
        deletedMessages = hashSetOf()
        messageList = mutableListOf()
        chatAdapter.submitList(messageList)
    }

    fun loadPreviousMessages() {
        currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER)?.let { channel ->
            if (chatRepository != null) {
                logDebug { "Chat loading previous messages size:${messageList.size},all Message size:${messageList.size},deleted Message:${deletedMessages.size}," }
                chatRepository?.loadPreviousMessages(channel)
            } else {
                eventStream.onNext(EVENT_LOADING_COMPLETE)
                logError { "Chat repo is null" }
            }
        }
    }

    fun uploadAndPostImage(context: Context, chatMessage: ChatMessage, timedata: EpochTime) {
        currentChatRoom?.chatroomMessageUrl?.let { sendMessageUrl ->
            val url =
                Uri.parse(chatMessage.message?.substring(1, (chatMessage.message?.length ?: 0) - 1))
            uiScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openAssetFileDescriptor(
                        url,
                        "r"
                    )?.use {
                        try {
                            val fileBytes = it.createInputStream().readBytes()
                            val imageUrl = dataClient.uploadImage(
                                currentChatRoom!!.uploadUrl,
                                null,
                                fileBytes
                            )
                            chatMessage.messageEvent = PubnubChatEventType.IMAGE_CREATED
                            chatMessage.imageUrl = imageUrl
                            val bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)
                            chatMessage.image_width = bitmap.width
                            chatMessage.image_height = bitmap.height
                            val m = chatMessage.copy()
                            m.message = ""
                            chatListener?.onChatMessageSend(sendMessageUrl, m, timedata)
                            bitmap.recycle()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            logError { e.message }
                            e.message?.let { it1 -> errorDelegate?.onError(it1) }
                        }
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    logError { e.message }
                    e.message?.let { it1 -> errorDelegate?.onError(it1) }
                }
            }
            Glide.with(context.applicationContext)
                .`as`(ByteArray::class.java)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : CustomTarget<ByteArray>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(
                        fileBytes: ByteArray,
                        transition: Transition<in ByteArray>?
                    ) {
                        try {
                            uiScope.launch(Dispatchers.IO) {
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            logError { e.message }
                        }
                    }
                })
        }
    }

    fun List<ChatMessage>.getIndexList(predicate: (ChatMessage) -> Boolean): List<Int> {
        val list = arrayListOf<Int>()
        this.forEachIndexed { index, chatMessage ->
            if (predicate(chatMessage)) {
                list.add(index)
            }
        }
        return list
    }

    companion object {
        const val EVENT_NEW_MESSAGE = "new-message"
        const val EVENT_MESSAGE_DELETED = "deletion"
        const val EVENT_MESSAGE_TIMETOKEN_UPDATED = "id-updated"
        const val EVENT_LOADING_COMPLETE = "loading-complete"
        const val EVENT_LOADING_STARTED = "loading-started"
        const val EVENT_REACTION_ADDED = "reaction-added"
        const val EVENT_REACTION_REMOVED = "reaction-removed"
        const val EVENT_MESSAGE_CANNOT_SEND =
            "message_cannot_send" // case 0 : occurs when user is muted inside a room and sends a message
    }
}
