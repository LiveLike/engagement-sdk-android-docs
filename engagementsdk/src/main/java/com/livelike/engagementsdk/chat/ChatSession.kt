package com.livelike.engagementsdk.chat

import android.content.Context
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.CHAT_HISTORY_LIMIT
import com.livelike.engagementsdk.CHAT_PROVIDER
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.MockAnalyticsService
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.chat.chatreaction.ChatReactionRepository
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.chat.services.messaging.pubnub.PubnubChatMessagingClient
import com.livelike.engagementsdk.chat.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.proxies.syncTo
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Created by Shivansh Mittal on 2020-04-08.
 */
internal class ChatSession(
    sdkConfiguration: Stream<EngagementSDK.SdkConfiguration>,
    private val userRepository: UserRepository,
    private val applicationContext: Context,
    private val publicRoom: ChatRoom? = null,
    private val errorDelegate: ErrorDelegate? = null,
    private val currentPlayheadTime: () -> EpochTime
) : LiveLikeChatSession {

    private var pubnubClientForMessageCount: PubnubChatMessagingClient? = null
    private lateinit var pubnubMessagingClient: PubnubChatMessagingClient
    // TODO get analytics service by moving it to SDK level instewad of program
    val analyticService: AnalyticsService = MockAnalyticsService()
    val chatViewModel: ChatViewModel by lazy { ChatViewModel(MockAnalyticsService(), userRepository.currentUserStream, publicRoom != null, null) }
    override var getActiveChatRoom: () -> String = { chatViewModel.currentChatRoom?.id ?: "" }
    private var chatClient: MessagingClient? = null
    private val contentSessionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var chatRoomMap = mutableMapOf<String, ChatRoom>()
    private val chatRoomMsgMap = mutableMapOf<String, List<ChatMessage>>()
    private var chatRepository: ChatRepository? = null
    private var privateChatRoomID = ""

    private val configurationUserPairFlow = flow {
        while (sdkConfiguration.latest() == null || userRepository.currentUserStream.latest() == null) {
            delay(1000)
        }
        emit(Pair(sdkConfiguration.latest()!!, userRepository.currentUserStream.latest()!!))
    }

    init {
        sdkConfiguration
            .subscribe(this.hashCode()) {
                it?.let {
                    val liveLikeUser = userRepository.currentUserStream.latest()!!
                    chatRepository =
                        ChatRepository(
                            it.pubNubKey,
                            liveLikeUser.accessToken,
                            liveLikeUser.id,
                            MockAnalyticsService(),
                            it.pubnubPublishKey,
                            origin = it.pubnubOrigin
                        )
                    logDebug { "chatRepository created" }
                    chatViewModel.reportUrl = null
                    chatViewModel.stickerPackRepository =
                        StickerPackRepository(it.clientId, it.stickerPackUrl)
                    chatViewModel.chatReactionRepository =
                        ChatReactionRepository(it.reactionPacksUrl)
                    chatViewModel.chatRepository = chatRepository
                    contentSessionScope.launch {
                        chatViewModel.chatReactionRepository?.preloadImages(
                            applicationContext
                        )
                    }
//                    if (privateChatRoomID.isEmpty()) {
//                        chatViewModel.currentChatRoom = program.defaultChatRoom
//                        initializeChatMessaging(program.defaultChatRoom?.channels?.chat?.get("pubnub"))
//                    }
                    initializeChatMessaging(publicRoom, currentPlayheadTime)
                }
            }
    }

    override fun pause() {
        chatClient?.start()
    }

    override fun resume() {
        chatClient?.start()
    }

    override fun close() {
        chatClient?.run {
            destroy()
        }
        contentSessionScope.cancel()
    }

    // TODO remove proxy message listener by having pipe in chat data layers/chain that tranforms pubnub channel to room
    private var proxyMsgListener: MessageListener = object : MessageListener {
        override fun onNewMessage(chatRoom: String, message: LiveLikeChatMessage) {
            logDebug {
                "ContentSession onNewMessage: ${message.message} timestamp:${message.timestamp}  chatRoomsSize:${chatRoomMap.size} chatRoomId:$chatRoom"
            }
            for (chatRoomIdPair in chatRoomMap) {
                if (chatRoomIdPair.value.channels.chat[CHAT_PROVIDER] == chatRoom) {
                    msgListener?.onNewMessage(chatRoomIdPair.key, message)
                    return
                }
            }
        }
    }

    private var msgListener: MessageListener? = null

    private fun initializeChatMessaging(
        chatRoom: ChatRoom?,
        currentPlayheadTime: () -> EpochTime
    ) {
        analyticService.trackLastChatStatus(true)
        chatClient = chatRepository?.establishChatMessagingConnection()

        pubnubMessagingClient = chatClient as PubnubChatMessagingClient

        currentPlayheadTime?.let {
            chatClient =
                chatClient?.syncTo(it)
        }
        chatClient = chatClient?.toChatQueue()
            ?.apply {
                msgListener = proxyMsgListener
                chatRoom?.channels?.chat?.get("pubnub")?.let {
                    subscribe(listOf(it))
                    chatViewModel.currentChatRoom = chatRoom
                }
                this.renderer = chatViewModel
                chatViewModel.chatLoaded = false
                chatViewModel.chatListener = this
            }
        logDebug { "initialized Chat Messaging , isPrivateGroupChat:${chatRoom == null}" }
    }

    private fun fetchChatRoom(chatRoomId: String, chatRoomResultCall: suspend (chatRoom: ChatRoom) -> Unit) {
        contentSessionScope.launch {
            configurationUserPairFlow.collect { pair ->
                chatRepository?.let { chatRepository ->
                    val chatRoomResult =
                        chatRepository.fetchChatRoom(chatRoomId, pair.first.chatRoomUrlTemplate)
                    if (chatRoomResult is Result.Success) {
                        chatRoomMap[chatRoomId] = chatRoomResult.data
                        chatRoomResultCall.invoke(chatRoomResult.data)
                    } else if (chatRoomResult is Result.Error) {
                        errorDelegate?.onError("error in fetching room id $chatRoomId")
                        logError {
                            chatRoomResult.exception?.message
                                ?: "error in fetching room id resource"
                        }
                    }
                }
            }
        }
    }
    override fun getMessageCount(
        chatRoomId: String,
        startTimestamp: Long,
        callback: LiveLikeCallback<Long>
    ) {
        fetchChatRoom(chatRoomId) { chatRoom ->
            chatRoom.channels.chat[CHAT_PROVIDER]?.let { channel ->
                if (pubnubClientForMessageCount == null) {
                    pubnubClientForMessageCount =
                        chatRepository?.establishChatMessagingConnection() as PubnubChatMessagingClient
                }
                pubnubClientForMessageCount?.getMessageCount(channel, startTimestamp)?.run {
                    callback.processResult(this)
                }
            }
        }
    }

    override fun joinChatRoom(chatRoomId: String, timestamp: Long) {
        logDebug { "joinChatRoom: $chatRoomId  timestamp:$timestamp" }
        if (chatRoomMap.size > 50) {
            return logError {
                "subscribing  count for pubnub channels cannot be greater than 50"
            }
        }
        if (chatRoomMap.containsKey(chatRoomId)) {
            return
        }
        fetchChatRoom(chatRoomId) {
            val channel = it.channels.chat[CHAT_PROVIDER]
            channel?.let { channel ->
                delay(500)
                pubnubMessagingClient?.addChannelSubscription(channel, timestamp)
            }
        }
    }

    override fun leaveChatRoom(chatRoomId: String) {
        chatRoomMap[chatRoomId]?.let { chatRoom ->
            chatRoomMap.remove(chatRoomId)
            chatClient?.unsubscribe(listOf(chatRoom.channels.chat[CHAT_PROVIDER] ?: ""))
        }
    }

    override fun enterChatRoom(chatRoomId: String) {
        logDebug { "Entering chatRoom $chatRoomId , currentChatRoom:$privateChatRoomID" }
        if (privateChatRoomID == chatRoomId) return // Already in the room
        val lastChatRoomId = privateChatRoomID
        privateChatRoomID = chatRoomId

        fetchChatRoom(chatRoomId) { chatRoom ->
            val channel = chatRoom.channels.chat[CHAT_PROVIDER] ?: ""
            delay(500)
            chatViewModel.apply {
                logDebug { "Chat caching message for chatRoom:$lastChatRoomId" }
                chatRoomMsgMap[lastChatRoomId] = messageList.takeLast(CHAT_HISTORY_LIMIT)
                flushMessages()
                if (chatRoomMsgMap.containsKey(chatRoomId)) {
                    logDebug { "Chat getting cache message from chatRoom:$chatRoomId" }
                    chatRoomMsgMap[chatRoomId]?.let {
                        this.cacheList.addAll(it)
                    }
                }
                currentChatRoom = chatRoom
                chatLoaded = false
            }
            pubnubMessagingClient?.activeChatRoom = channel
        }
    }

    override fun exitChatRoom(chatRoomId: String) {
        leaveChatRoom(chatRoomId)
        chatViewModel.apply {
            flushMessages()
        }
    }

    override fun exitAllConnectedChatRooms() {
        chatClient?.unsubscribeAll()
    }

    override fun setMessageListener(
        messageListener: MessageListener
    ) {
        msgListener = messageListener
    }
}
