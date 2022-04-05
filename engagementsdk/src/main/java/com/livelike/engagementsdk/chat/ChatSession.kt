package com.livelike.engagementsdk.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.livelike.engagementsdk.*
import com.livelike.engagementsdk.chat.chatreaction.ChatReactionRepository
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.remote.PinMessageInfo
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.chat.services.messaging.pubnub.PubnubChatMessagingClient
import com.livelike.engagementsdk.chat.services.network.ChatDataClient
import com.livelike.engagementsdk.chat.services.network.ChatDataClientImpl
import com.livelike.engagementsdk.chat.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.proxies.syncTo
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.publicapis.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.net.URL
import java.util.*


internal class ChatSession(
    sdkConfiguration: Stream<EngagementSDK.SdkConfiguration>,
    private val userRepository: UserRepository,
    private val applicationContext: Context,
    private val isPublicRoom: Boolean = true,
    internal val analyticsServiceStream: Stream<AnalyticsService>,
    internal val errorDelegate: ErrorDelegate? = null,
    private val liveLikeChatClient: LiveLikeChatClient,
    private val currentPlayheadTime: () -> EpochTime
) : LiveLikeChatSession {

    override fun getPlayheadTime(): EpochTime {
        return currentPlayheadTime.invoke()
    }

    private var pubnubClientForMessageCount: PubnubChatMessagingClient? = null
    private var pubnubMessagingClient: PubnubChatMessagingClient? = null
    internal val dataClient: ChatDataClient = ChatDataClientImpl()
    private var isClosed = false
    val chatViewModel: ChatViewModel by lazy {
        ChatViewModel(
            applicationContext,
            userRepository.currentUserStream,
            isPublicRoom,
            null,
            dataClient = dataClient,
            errorDelegate = errorDelegate
        )
    }
    override var getCurrentChatRoom: () -> String = { currentChatRoom?.id ?: "" }

    private var chatClient: MessagingClient? = null
    private val contentSessionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var chatRepository: ChatRepository? = null
    private var chatRoomId: String? = null

    private val chatSessionIdleStream: Stream<Boolean> =
        SubscriptionManager(true)
    private var currentChatRoom: ChatRoom? = null
    private val messages = ArrayList<LiveLikeChatMessage>()
    private val deletedMsgList = arrayListOf<String>()

    private val configurationUserPairFlow = flow {
        while (sdkConfiguration.latest() == null || userRepository.currentUserStream.latest() == null) {
            delay(1000)
        }
        emit(Pair(sdkConfiguration.latest()!!, userRepository.currentUserStream.latest()!!))
    }

    init {
        contentSessionScope.launch {
            configurationUserPairFlow.collect { pair ->
                chatViewModel.analyticsService = analyticsServiceStream.latest()!!
                val liveLikeUser = pair.second
                chatRepository =
                    ChatRepository(
                        pair.first.pubNubKey,
                        liveLikeUser.accessToken,
                        liveLikeUser.id,
                        MockAnalyticsService(),
                        pair.first.pubnubPublishKey,
                        origin = pair.first.pubnubOrigin,
                        pubnubHeartbeatInterval = pair.first.pubnubHeartbeatInterval,
                        pubnubPresenceTimeout = pair.first.pubnubPresenceTimeout
                    )
                logDebug { "chatRepository created" }
                // updating urls value will be added in enterChat Room
                chatViewModel.liveLikeChatClient = liveLikeChatClient
                chatViewModel.chatRepository = chatRepository
                initializeChatMessaging(currentPlayheadTime)
                chatSessionIdleStream.onNext(true)
            }
        }
    }

    override var shouldDisplayAvatar: Boolean
        get() = chatViewModel.showChatAvatarLogo
        set(value) {
            chatViewModel.showChatAvatarLogo = value
        }

    private fun updatingURls(
        clientId: String,
        stickerPackUrl: String,
        reactionPacksUrl: String,
        reportUrl: String?
    ) {
        if (isClosed) {
            logError { "Session is closed" }
            errorDelegate?.onError("Session is closed")
            return
        }
        contentSessionScope.launch {
            configurationUserPairFlow.collect { pair ->
                chatViewModel.stickerPackRepository =
                    StickerPackRepository(clientId, stickerPackUrl)
                chatViewModel.chatReactionRepository =
                    ChatReactionRepository(reactionPacksUrl, pair.second.accessToken)
                chatViewModel.reportUrl = reportUrl
                contentSessionScope.launch {
                    chatViewModel.chatReactionRepository?.preloadImages(
                        applicationContext
                    )
                }
            }
        }
    }

    override fun pause() {
        chatClient?.stop()
    }

    override fun resume() {
        chatClient?.start()
    }

    override fun close() {
        chatClient?.run {
            destroy()
        }
        (liveLikeChatClient as InternalLiveLikeChatClient).unsubscribeToChatRoomDelegate(
            chatViewModel.hashCode().toString()
        )
        contentSessionScope.cancel()
        isClosed = true
        chatViewModel.chatAdapter.mRecyclerView = null
    }

    // TODO remove proxy message listener by having pipe in chat data layers/chain that tranforms pubnub channel to room
    private var proxyMsgListener: MessageListener = object : MessageListener {
        override fun onNewMessage(message: LiveLikeChatMessage) {
            logDebug {
                "ContentSession onNewMessage: ${message.message} timestamp:${message.timestamp}"
            }
            this@ChatSession.messages.add(message)
            msgListener?.onNewMessage(message)
        }

        override fun onHistoryMessage(messages: List<LiveLikeChatMessage>) {
            this@ChatSession.messages.addAll(0, messages)
            msgListener?.onHistoryMessage(messages)
        }

        override fun onDeleteMessage(messageId: String) {
            deletedMsgList.add(messageId)
            msgListener?.onDeleteMessage(messageId)
        }

        override fun onPinMessage(message: PinMessageInfo) {
            msgListener?.onPinMessage(message)
        }

        override fun onUnPinMessage(pinMessageId: String) {
            msgListener?.onUnPinMessage(pinMessageId)
        }

        override fun onErrorMessage(error: String, clientMessageId: String?) {
            msgListener?.onErrorMessage(error, clientMessageId)
        }
    }

    private var msgListener: MessageListener? = null
    private var chatRoomListener: ChatRoomListener? = null

    private val proxyChatRoomListener = object : ChatRoomListener {
        override fun onChatRoomUpdate(chatRoom: ChatRoomInfo) {
            chatRoomListener?.onChatRoomUpdate(chatRoom)
        }
    }

    private fun initializeChatMessaging(
        currentPlayheadTime: () -> EpochTime
    ) {
        analyticsServiceStream.latest()!!.trackLastChatStatus(true)
        chatClient = chatRepository?.establishChatMessagingConnection()
        pubnubMessagingClient = chatClient as PubnubChatMessagingClient
        currentPlayheadTime.let {
            chatClient =
                chatClient?.syncTo(it)
        }
        chatClient = chatClient?.toChatQueue()
            ?.apply {
                this.liveLikeChatClient = this@ChatSession.liveLikeChatClient
                msgListener = proxyMsgListener
                chatRoomListener = this@ChatSession.proxyChatRoomListener
                this.renderer = chatViewModel
                chatViewModel.chatLoaded = false
                chatViewModel.chatListener = this
                pubnubMessagingClient?.isDiscardOwnPublishInSubscription =
                    allowDiscardOwnPublishedMessageInSubscription
            }
        logDebug { "initialized Chat Messaging" }
    }

    private fun fetchChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoom>
    ) {
        chatSessionIdleStream.subscribe(chatRoomId) {
            if (it == true) {
                if (isClosed) {
                    logError { "Session is closed" }
                    errorDelegate?.onError("Session is closed")
                    return@subscribe
                }
                contentSessionScope.launch {
                    configurationUserPairFlow.collect { pair ->
                        logDebug { "fetch ChatRoom" }
                        chatRepository?.let { chatRepository ->
                            val chatRoomResult =
                                chatRepository.fetchChatRoom(
                                    chatRoomId,
                                    pair.first.chatRoomDetailUrlTemplate
                                )
                            if (chatRoomResult is Result.Success) {
                                liveLikeCallback.onResponse(chatRoomResult.data, null)
                            } else if (chatRoomResult is Result.Error) {
                                errorDelegate?.onError("error in fetching room id $chatRoomId")
                                liveLikeCallback.onResponse(
                                    null,
                                    chatRoomResult.exception.message
                                        ?: "error in fetching room id resource"
                                )
                                logError {
                                    chatRoomResult.exception.message
                                        ?: "error in fetching room id resource"
                                }
                            }
                            chatSessionIdleStream.unsubscribe(chatRoomId)
                        }
                    }
                }
            }
        }
    }

    override fun getMessageCount(
        startTimestamp: Long,
        callback: LiveLikeCallback<Byte>
    ) {
        chatRoomId?.let {
            logDebug { "messageCount $chatRoomId ,$startTimestamp" }
            fetchChatRoom(
                it,
                object : LiveLikeCallback<ChatRoom>() {
                    override fun onResponse(result: ChatRoom?, error: String?) {
                        result?.let { chatRoom ->
                            chatRoom.channels.chat[CHAT_PROVIDER]?.let { channel ->
                                if (pubnubClientForMessageCount == null) {
                                    pubnubClientForMessageCount =
                                        chatRepository?.establishChatMessagingConnection() as PubnubChatMessagingClient
                                }
                                pubnubClientForMessageCount?.getMessageCountV1(
                                    channel,
                                    startTimestamp
                                )
                                    ?.run {
                                        callback.processResult(this)
                                    }
                            }
                        }
                        error?.let {
                            callback.onResponse(null, error)
                        }
                    }
                }
            )
        }
    }

    // TODO: will move to constructor later after discussion
    override fun connectToChatRoom(chatRoomId: String, callback: LiveLikeCallback<Unit>?) {
        if (chatRoomId.isEmpty()) {
            callback?.onResponse(null, "ChatRoom Id cannot be Empty")
            errorDelegate?.onError("ChatRoom Id cannot be Empty")
            return
        }
        if (currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER) == chatRoomId) return // Already in the room
        currentChatRoom?.let { chatRoom ->
            chatClient?.unsubscribe(listOf(chatRoom.channels.chat[CHAT_PROVIDER] ?: ""))
        }
        chatViewModel.apply {
            flushMessages()
        }
        messages.clear()
        deletedMsgList.clear()
        this.chatRoomId = chatRoomId
        fetchChatRoom(
            chatRoomId,
            object : LiveLikeCallback<ChatRoom>() {
                override fun onResponse(result: ChatRoom?, error: String?) {
                    result?.let { chatRoom ->
                        //subscribe to channel for listening for pin message events
                        val controlChannel = chatRoom.channels.control[CHAT_PROVIDER]
                        controlChannel?.let {
                            pubnubMessagingClient?.addChannelSubscription(it)
                        }
                        val channel = chatRoom.channels.chat[CHAT_PROVIDER]
                        channel?.let { ch ->
                            contentSessionScope.launch {
                                delay(500)
                                pubnubMessagingClient?.addChannelSubscription(ch)
                                delay(500)
                                chatViewModel.apply {
                                    flushMessages()
                                    updatingURls(
                                        chatRoom.clientId,
                                        chatRoom.stickerPacksUrl,
                                        chatRoom.reactionPacksUrl,
                                        chatRoom.reportMessageUrl
                                    )
                                    delay(1000)
                                    currentChatRoom = chatRoom
                                    chatLoaded = false
                                }
                                this@ChatSession.currentChatRoom = chatRoom
                                pubnubMessagingClient?.activeChatRoom = channel
                                callback?.onResponse(Unit, null)
                            }
                        }
                    }
                    error?.let {
                        callback?.onResponse(null, error)
                    }
                }
            }
        )
    }

    override fun setMessageListener(
        messageListener: MessageListener
    ) {
        msgListener = messageListener
    }

    override fun setChatRoomListener(chatRoomListener: ChatRoomListener) {
        this.chatRoomListener = chatRoomListener
    }

    override var avatarUrl: String? = null

    override var allowDiscardOwnPublishedMessageInSubscription: Boolean = true
        set(value) {
            field = value
            pubnubMessagingClient?.isDiscardOwnPublishInSubscription = value
        }

    /**
     * TODO: added it into default chat once all functionality related to chat is done
     */
    override fun sendMessage(
        message: String?,
        imageUrl: String?,
        imageWidth: Int?,
        imageHeight: Int?,
        liveLikePreCallback: LiveLikeCallback<LiveLikeChatMessage>
    ) {
        internalSendMessage(
            message,
            imageUrl,
            imageWidth,
            imageHeight,
            preLiveLikeCallback = liveLikePreCallback
        )
    }

    override fun quoteMessage(
        message: String?,
        imageUrl: String?,
        imageWidth: Int?,
        imageHeight: Int?,
        quoteMessageId: String,
        quoteMessage: LiveLikeChatMessage,
        liveLikePreCallback: LiveLikeCallback<LiveLikeChatMessage>
    ) {
        // Removing the parent message from parent message in order to avoid reply to reply in terms of data
        // and avoid data nesting
        if (quoteMessage.quoteMessage != null) {
            quoteMessage.quoteMessage = null
        }
        internalSendMessage(
            message,
            imageUrl,
            imageWidth,
            imageHeight,
            quoteMessage,
            liveLikePreCallback
        )
    }

    private fun internalSendMessage(
        message: String?,
        imageUrl: String?,
        imageWidth: Int?,
        imageHeight: Int?,
        parentChatMessage: LiveLikeChatMessage? = null,
        preLiveLikeCallback: LiveLikeCallback<LiveLikeChatMessage>
    ) {
        if (message?.isEmpty() == true) {
            preLiveLikeCallback.onResponse(null, "Message cannot be empty")
            return
        }
        val timeData = getPlayheadTime()
        ChatMessage(
            when (imageUrl != null) {
                true -> PubnubChatEventType.IMAGE_CREATED
                else -> PubnubChatEventType.MESSAGE_CREATED
            },
            currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER) ?: "",
            message,
            "",
            userRepository.currentUserStream.latest()?.id ?: "empty-id",
            userRepository.currentUserStream.latest()?.nickname ?: "John Doe",
            avatarUrl,
            imageUrl = imageUrl,
            isFromMe = true,
            image_width = imageWidth ?: 100,
            image_height = imageHeight ?: 100,
            timeStamp = timeData.timeSinceEpochInMs.toString(),
            quoteMessage = parentChatMessage?.copy()?.toChatMessage(),
            clientMessageId = UUID.randomUUID().toString(),
        ).let { chatMessage ->
            // TODO: need to update for error handling here if pubnub respond failure of message
            preLiveLikeCallback.onResponse(chatMessage.toLiveLikeChatMessage(), null)
            currentChatRoom?.chatroomMessageUrl?.let { messageUrl ->
                val hasExternalImage = imageUrl != null
                if (hasExternalImage) {
                    contentSessionScope.launch {
                        val uri = Uri.parse(chatMessage.imageUrl)
                        when {
                            uri.scheme != null && uri.scheme.equals("content") -> {
                                applicationContext.contentResolver.openInputStream(uri)
                            }
                            else -> {
                                URL(chatMessage.imageUrl).openConnection().getInputStream()
                            }
                        }?.use {
                            val fileBytes = it.readBytes()
                            val uploadedImageUrl = dataClient.uploadImage(
                                currentChatRoom!!.uploadUrl,
                                null,
                                fileBytes
                            )
                            chatMessage.messageEvent = PubnubChatEventType.IMAGE_CREATED
                            chatMessage.imageUrl = uploadedImageUrl
                            val bitmap =
                                BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)
                            chatMessage.image_width = imageWidth ?: bitmap.width
                            chatMessage.image_height = imageHeight ?: bitmap.height
                            val m = chatMessage.copy()
                            m.message = ""
                            (chatClient as? ChatEventListener)?.onChatMessageSend(
                                messageUrl,
                                chatMessage,
                                timeData
                            )
                            bitmap.recycle()
                        }
                    }
                } else {
                    (chatClient as? ChatEventListener)?.onChatMessageSend(
                        messageUrl,
                        chatMessage,
                        timeData
                    )
                }
                currentChatRoom?.id?.let { id ->
                    chatMessage.id?.let {
                        analyticsServiceStream.latest()?.trackMessageSent(
                            it,
                            chatMessage.message,
                            hasExternalImage,
                            id
                        )
                    }
                }
            }
        }
    }

    override fun loadNextHistory(limit: Int) {
        currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER)?.let { channel ->
            if (chatRepository != null) {
                chatRepository?.loadPreviousMessages(channel, limit)
            } else {
                logError { "Chat repo is null" }
                errorDelegate?.onError("Chat Repository is Null")
            }
        }
    }

    override fun getLoadedMessages(): ArrayList<LiveLikeChatMessage> {
        return messages
    }

    override fun getDeletedMessages(): ArrayList<String> {
        return deletedMsgList
    }

    override fun sendCustomChatMessage(
        customData: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeChatMessage>
    ) {
        currentChatRoom?.customMessagesUrl?.let { url ->
            contentSessionScope.launch {
                if (chatRepository != null) {
                    val jsonObject = JSONObject(
                        mapOf("custom_data" to customData)
                    )
                    val response = chatRepository!!.postApi(url, jsonObject.toString())
                    liveLikeCallback.processResult(response)
                } else {
                    logError { "Chat repo is null" }
                    errorDelegate?.onError("Chat Repository is Null")
                }
            }
        }
    }
}
