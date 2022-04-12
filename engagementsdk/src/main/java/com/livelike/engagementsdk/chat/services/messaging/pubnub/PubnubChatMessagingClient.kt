package com.livelike.engagementsdk.chat.services.messaging.pubnub

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.*
import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.ChatMessageReaction
import com.livelike.engagementsdk.chat.ChatViewModel
import com.livelike.engagementsdk.chat.MessageError
import com.livelike.engagementsdk.chat.data.remote.*
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.*
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.chat.data.toChatMessage
import com.livelike.engagementsdk.chat.data.toPubnubChatMessage
import com.livelike.engagementsdk.chat.utils.liveLikeSharedPrefs.addPublishedMessage
import com.livelike.engagementsdk.chat.utils.liveLikeSharedPrefs.flushPublishedMessage
import com.livelike.engagementsdk.chat.utils.liveLikeSharedPrefs.getPublishedMessages
import com.livelike.engagementsdk.core.services.messaging.*
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.*
import com.livelike.engagementsdk.core.utils.Queue
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getSharedPreferences
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.services.messaging.pubnub.PubnubSubscribeCallbackAdapter
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.PubNubException
import com.pubnub.api.enums.PNOperationType
import com.pubnub.api.enums.PNReconnectionPolicy
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.message_actions.PNMessageAction
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import kotlinx.coroutines.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class PubnubChatMessagingClient(
    subscriberKey: String,
    authKey: String,
    uuid: String,
    private val analyticsService: AnalyticsService,
    publishKey: String? = null,
    var isDiscardOwnPublishInSubscription: Boolean = true,
    val origin: String? = null,
    private val pubnubHeartbeatInterval: Int,
    private val pubnubPresenceTimeout: Int,
    private val chatRepository: ChatRepository
) : MessagingClient {

    @Volatile
    private var lastActionTimeToken: Long = 0
    private var connectedChannels: MutableSet<String> = mutableSetOf()

    private val publishQueue =
        Queue<Pair<String, PubnubChatEvent<PubnubChatMessage>>>()

    private val coroutineScope = MainScope()
    private var isPublishRunning = false
    private var firstTimeToken: Long = 0
    private var pubnubChatRoomLastMessageTime: MutableMap<String, ArrayList<String>>? = null

    var activeChatRoom: ChatRoom? = null
        set(value) {
            field = value
            firstTimeToken = 0
            if (value != null) {
                value.channels.chat[CHAT_PROVIDER]?.let {
                    subscribe(listOf(it))
                }
                flushPublishedMessage(*connectedChannels.toTypedArray())
            }
        }

    @Synchronized
    fun addChannelSubscription(channel: String) {
        if (!connectedChannels.contains(channel)) {
            connectedChannels.add(channel)
            flushPublishedMessage(*connectedChannels.toTypedArray())
            pubnub.subscribe().channels(listOf(channel)).execute()
        }
    }

    private fun convertToTimeToken(timestamp: Long): Long {
        return timestamp * 10000
    }

    override fun publishMessage(
        url: String,
        message: String,
        channel: String,
        timeSinceEpoch: EpochTime
    ) {
        val clientMessage = gson.fromJson(message, ChatMessage::class.java)
        val pubnubChatEvent = PubnubChatEvent(
            clientMessage.messageEvent.key,
            clientMessage.toPubnubChatMessage(
                when (timeSinceEpoch.timeSinceEpochInMs) {
                    0L -> null
                    else -> ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(timeSinceEpoch.timeSinceEpochInMs),
                        org.threeten.bp.ZoneId.of("UTC")
                    ).format(isoUTCDateTimeFormatter)
                }
            ),
            null,
            url
        )
        publishQueue.enqueue(Pair(channel, pubnubChatEvent))
        if (isDiscardOwnPublishInSubscription) {
            addPublishedMessage(channel, pubnubChatEvent.payload.clientMessageId!!)
        }
        if (!isPublishRunning) {
            startPublishingFromQueue()
        }
    }

    private fun startPublishingFromQueue() {
        isPublishRunning = true
        coroutineScope.async {
            while (!publishQueue.isEmpty()) {
                publishQueue.peek()?.let { messageChannelPair ->
                    if (publishMessageToServer(
                            messageChannelPair.second,
                            messageChannelPair.first
                        )
                    ) {
                        publishQueue.dequeue()
                        delay(100) // ensure messages not more than 5 per second as 100ms is pubnub latency
                    } else {
                        delay(2000) // Linear back-off strategy.
                    }
                }
            }
            isPublishRunning = false
        }
    }

    private suspend fun publishMessageToServer(
        pubnubChatEvent: PubnubChatEvent<PubnubChatMessage>,
        channel: String
    ) = suspendCoroutine<Boolean> {
        coroutineScope.launch {
            println("PubnubChatMessagingClient.publishMessageToServer>>${pubnubChatEvent.messageUrl}>>${pubnubChatEvent.payload.chatRoomId}")
            val result = chatRepository.sendMessage(
                pubnubChatEvent.messageUrl,
                pubnubChatEvent.payload.apply {
                    messageEvent = pubnubChatEvent.pubnubChatEventType
                })
            if (result is Result.Error) {
                listener?.onClientMessageError(
                    this@PubnubChatMessagingClient,
                    Error(
                        MessageError.DENIED_MESSAGE_PUBLISH.name,
                        result.exception.toString(),
                        pubnubChatEvent.payload.clientMessageId
                    )
                )
                it.resume(true)
            } else if (result is Result.Success) {
                //TODO: has to uncomment later once reaction api is implemented
//                processReceivedMessage(
//                    channel,
//                    PubnubChatEvent(
//                        result.data.messageEvent!!,
//                        result.data,
//                        0L,
//                        pubnubChatEvent.messageUrl
//                    ),
//                    0L
//                )
                it.resume(true)
            }
        }
    }

    override fun stop() {
        pubnub.disconnect()
    }

    override fun start() {
        pubnub.reconnect()
    }

    private val pubnubConfiguration: PNConfiguration = PNConfiguration()
    var pubnub: PubNub
    private var listener: MessagingEventListener? = null

    init {
        pubnubConfiguration.subscribeKey = subscriberKey
        pubnubConfiguration.authKey = authKey
        pubnubConfiguration.uuid = uuid
        pubnubConfiguration.publishKey = publishKey
        pubnubConfiguration.filterExpression =
            "sender_id == '$uuid' || !(content_filter contains 'filtered')"
        if (origin != null) {
            pubnubConfiguration.origin = origin
        }
        pubnubConfiguration.setPresenceTimeoutWithCustomInterval(
            pubnubPresenceTimeout,
            pubnubHeartbeatInterval
        )
        pubnubConfiguration.reconnectionPolicy = PNReconnectionPolicy.EXPONENTIAL
        pubnub = PubNub(pubnubConfiguration)
        val client = this

        // Extract SubscribeCallback?
        pubnub.addListener(object : PubnubSubscribeCallbackAdapter() {
            override fun status(pubnub: PubNub, pnStatus: PNStatus) {
                when (pnStatus.operation) {
                    // let's combine unsubscribe and subscribe handling for ease of use
                    PNOperationType.PNSubscribeOperation, PNOperationType.PNUnsubscribeOperation -> {
                        // note: subscribe statuses never have traditional
                        // errors, they just have categories to represent the
                        // different issues or successes that occur as part of subscribe
                        when (pnStatus.category) {
                            PNStatusCategory.PNConnectedCategory -> {
                                // this is expected for a subscribe, this means there is no error or issue whatsoever
                                listener?.onClientMessageStatus(client, ConnectionStatus.CONNECTED)
                            }

                            PNStatusCategory.PNReconnectedCategory -> {
                                // this usually occurs if subscribe temporarily fails but reconnects. This means
                                // there was an error but there is no longer any issue
                                listener?.onClientMessageStatus(client, ConnectionStatus.CONNECTED)
                            }

                            PNStatusCategory.PNDisconnectedCategory -> {
                                // this is the expected category for an unsubscribe. This means there
                                // was no error in unsubscribing from everything
                                listener?.onClientMessageStatus(
                                    client,
                                    ConnectionStatus.DISCONNECTED
                                )
                            }

                            PNStatusCategory.PNAccessDeniedCategory -> {
                                // this means that PAM does allow this client to subscribe to this
                                // channel and channel group configuration. This is another explicit error
                                listener?.onClientMessageError(
                                    client,
                                    Error("Access Denied", "Access Denied", null)
                                )
                            }

                            PNStatusCategory.PNTimeoutCategory, PNStatusCategory.PNNetworkIssuesCategory, PNStatusCategory.PNUnexpectedDisconnectCategory -> {
                                // this is usually an issue with the internet connection, this is an error, handle appropriately
                                pubnub.reconnect()
                            }

                            else -> {
                                // Some other category we have yet to handle
                            }
                        }
                    }

                    else -> {
                        // some other Operation Type we are not handling yet.
                    }
                }
            }

            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                val channel = pnMessageResult.channel
                val pubnubChatEvent: PubnubChatEvent<PubnubChatMessage> =
                    gson.fromJson(
                        pnMessageResult.message.asJsonObject,
                        object : TypeToken<PubnubChatEvent<PubnubChatMessage>>() {}.type
                    )
                processReceivedMessage(channel, pubnubChatEvent, pnMessageResult.timetoken)
            }

            override fun messageAction(
                pubnub: PubNub,
                pnMessageActionResult: PNMessageActionResult
            ) {
                // check later using coroutine mainscope not executing when private group, so removing for now
                lastActionTimeToken = pnMessageActionResult.messageAction.actionTimetoken
                logDebug { "real time message action : " + pnMessageActionResult.event }
                processPubnubMessageAction(pnMessageActionResult, client)
            }

            override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {
            }
        })
    }

    private fun processReceivedMessage(
        channel: String,
        pubnubChatEvent: PubnubChatEvent<PubnubChatMessage>,
        timeToken: Long
    ) {
        if (pubnubChatRoomLastMessageTime == null)
            pubnubChatRoomLastMessageTime = GsonBuilder().create().fromJson(
                getSharedPreferences()
                    .getString(
                        PREF_CHAT_ROOM_MSG_RECEIVED,
                        null
                    ),
                object : TypeToken<MutableMap<String, ArrayList<String>>>() {}.type
            ) ?: mutableMapOf()
        pubnubChatRoomLastMessageTime?.let { map ->
            val msgId = pubnubChatEvent.payload.messageId
            val list =
                when {
                    map.containsKey(channel) -> map[channel]
                    else -> ArrayList()
                } ?: ArrayList()
            when (pubnubChatEvent.pubnubChatEventType.toPubnubChatEventType()) {
                MESSAGE_CREATED, IMAGE_CREATED, CUSTOM_MESSAGE_CREATED -> {
                    if (!list.contains(msgId)) {
                        list.add(msgId!!)
                        map[channel] = list
                        getSharedPreferences()
                            .edit()
                            .putString(
                                PREF_CHAT_ROOM_MSG_RECEIVED,
                                GsonBuilder().create().toJson(map)
                            ).apply()
                        processPubnubChatEvent(
                            JsonParser.parseString(gson.toJson(pubnubChatEvent)).asJsonObject.apply {
                                addProperty("pubnubToken", timeToken)
                            },
                            channel, timeToken
                        )?.let {
                            listener?.onClientMessageEvent(this, it)
                        }
                    } else {
                        logDebug { "Received Message is already added in the list" }
                    }
                }
                else -> {
                    processPubnubChatEvent(
                        JsonParser.parseString(gson.toJson(pubnubChatEvent)).asJsonObject.apply {
                            addProperty("pubnubToken", timeToken)
                        },
                        channel, timeToken
                    )?.let {
                        listener?.onClientMessageEvent(this, it)
                    }
                }
            }
        }
    }

    private fun processPubnubMessageAction(
        pnMessageActionResult: PNMessageActionResult,
        client: PubnubChatMessagingClient
    ) {
        if (pnMessageActionResult.messageAction.type == REACTION_CREATED && pubnub.configuration.uuid != pnMessageActionResult.messageAction.uuid) {
            val clientMessage: ClientMessage
            if (pnMessageActionResult.event == "added") {
                clientMessage = ClientMessage(
                    JsonObject().apply {
                        addProperty("event", ChatViewModel.EVENT_REACTION_ADDED)
                        addProperty(
                            "isOwnReaction",
                            pubnub.configuration.uuid == pnMessageActionResult.messageAction.uuid
                        )
                        addProperty(
                            "actionPubnubToken",
                            pnMessageActionResult.messageAction.actionTimetoken
                        )
                        addProperty(
                            "messagePubnubToken",
                            pnMessageActionResult.messageAction.messageTimetoken
                        )
                        addProperty("emojiId", pnMessageActionResult.messageAction.value)
                    },
                    pnMessageActionResult.channel
                )
                listener?.onClientMessageEvent(client, clientMessage)
            } else {
                clientMessage = ClientMessage(
                    JsonObject().apply {
                        addProperty("event", ChatViewModel.EVENT_REACTION_REMOVED)
                        addProperty(
                            "messagePubnubToken",
                            pnMessageActionResult.messageAction.messageTimetoken
                        )
                        addProperty("emojiId", pnMessageActionResult.messageAction.value)
                    },
                    pnMessageActionResult.channel
                )
                listener?.onClientMessageEvent(client, clientMessage)
            }
        }
    }

    private fun processPubnubChatEvent(
        jsonObject: JsonObject,
        channel: String,
        timeToken: Long,
        actions: Map<String, List<PubnubChatReaction>>? = null
    ): ClientMessage? {
        val event = jsonObject.extractStringOrEmpty("event").toPubnubChatEventType()
        if (event != null) {
            val pubnubChatEvent: PubnubChatEvent<PubnubChatMessage> = gson.fromJson(
                jsonObject,
                object : TypeToken<PubnubChatEvent<PubnubChatMessage>>() {}.type
            )
            var clientMessage: ClientMessage? = null
            when (event) {
                MESSAGE_CREATED, IMAGE_CREATED, CUSTOM_MESSAGE_CREATED -> {
                    if (isDiscardOwnPublishInSubscription && getPublishedMessages(channel).contains(
                            pubnubChatEvent.payload.clientMessageId
                        )
                    ) {
                        logError { "discarding as its own recently published message which is broadcasted by pubnub on that channel." }
                        clientMessage = ClientMessage(
                            gson.toJsonTree(
                                pubnubChatEvent.payload.toChatMessage(
                                    channel,
                                    timeToken,
                                    processReactionCounts(actions),
                                    getOwnReaction(actions),
                                    event
                                )
                            ).asJsonObject.apply {
                                addProperty("event", ChatViewModel.EVENT_MESSAGE_TIMETOKEN_UPDATED)
                                addProperty("messageId", pubnubChatEvent.payload.messageId)
                                addProperty(
                                    "clientMessageId",
                                    pubnubChatEvent.payload.clientMessageId
                                )
                                addProperty("timeToken", timeToken)
                                addProperty("createdAt", pubnubChatEvent.payload.createdAt)
                            },
                            channel
                        )
                        return clientMessage
                    }
                    val pdtString = pubnubChatEvent.payload.programDateTime
                    var epochTimeMs = 0L
                    pdtString?.parseISODateTime()?.let {
                        epochTimeMs = it.toInstant().toEpochMilli()
                    }

                    try {
                        clientMessage = ClientMessage(
                            gson.toJsonTree(
                                pubnubChatEvent.payload.toChatMessage(
                                    channel,
                                    timeToken,
                                    processReactionCounts(actions),
                                    getOwnReaction(actions),
                                    event
                                )
                            ).asJsonObject.apply {
                                addProperty("event", ChatViewModel.EVENT_NEW_MESSAGE)
                                addProperty("pubnubMessageToken", pubnubChatEvent.pubnubToken)
                                addProperty("createdAt", pubnubChatEvent.payload.createdAt)
                            },
                            channel,
                            EpochTime(epochTimeMs)
                        )
                    } catch (ex: IllegalArgumentException) {
                        logError { ex.message }
                        return null
                    }
                }
                MESSAGE_DELETED, IMAGE_DELETED -> {
                    clientMessage = ClientMessage(
                        JsonObject().apply {
                            addProperty("event", ChatViewModel.EVENT_MESSAGE_DELETED)
                            addProperty("id", pubnubChatEvent.payload.messageId)
                        },
                        channel,
                        EpochTime(0)
                    )
                }
                CHATROOM_UPDATED -> {
                    val pubnubChatRoomEvent: PubnubChatEvent<ChatRoom> = gson.fromJson(
                        jsonObject,
                        object : TypeToken<PubnubChatEvent<ChatRoom>>() {}.type
                    )
                    clientMessage = ClientMessage(
                        gson.toJsonTree(pubnubChatRoomEvent.payload).asJsonObject.apply {
                            addProperty("event", CHATROOM_UPDATED.key)
                        },
                        channel
                    )
                }
                PubnubChatEventType.CHATROOM_ADDED -> {

                }
                PubnubChatEventType.CHATROOM_INVITE -> {

                }
                MESSAGE_PINNED, MESSAGE_UNPINNED -> {
                    val pubnubChatRoomEvent: PubnubChatEvent<PubnubPinMessageInfo> = gson.fromJson(
                        jsonObject,
                        object : TypeToken<PubnubChatEvent<PubnubPinMessageInfo>>() {}.type
                    )
                    clientMessage = ClientMessage(
                        gson.toJsonTree(pubnubChatRoomEvent.payload.toPinMessageInfo()).asJsonObject.apply {
                            addProperty("event", event.key)
                        },
                        channel
                    )
                }
            }
            logDebug { "Received message on $channel from pubnub: ${pubnubChatEvent.payload}" }
            return clientMessage
        } else {
            logError { "We don't know how to handle this message" }
        }
        return null
    }

    private fun isMessageModerated(chatMessage: PubnubChatMessage): Boolean {
        // added this check since in payload content filter was coming as string (json primitive) instead of array
        val contentFilter = chatMessage.contentFilter
        return (contentFilter?.size ?: 0) > 0
    }

    private fun getOwnReaction(actions: Map<String, List<PubnubChatReaction>>?): ChatMessageReaction? {
        actions?.let {
            for (value in actions.keys) {
                actions[value]?.forEach { action ->
                    if (action.uuid == pubnub.configuration.uuid) {
                        return ChatMessageReaction(value, action.actionTimeToken)
                    }
                }
            }
        }
        return null
    }

    private fun processReactionCounts(actions: Map<String, List<PubnubChatReaction>>?): MutableMap<String, Int> {
        val reactionCountMap = mutableMapOf<String, Int>()
        actions?.let {
            for (value in actions.keys) {
                reactionCountMap[value] = actions[value]?.size ?: 0
            }
        }
        return reactionCountMap
    }

    private var firstUntil: String? = null

    internal fun loadMessagesWithReactionsFromServer(chatHistoryLimit: Int = CHAT_HISTORY_LIMIT) {
        activeChatRoom?.let { chatRoom ->
            chatRoom.chatroomMessageUrl?.let { url ->
                coroutineScope.launch {
                    val result = chatRepository.getMessageHistory(
                        url,
                        chatRoom.id,
                        pageSize = chatHistoryLimit,
                        until = firstUntil
                    )
                    if (result is Result.Success) {
                        if (result.data.results.isNotEmpty()) {
                            firstUntil = result.data.results.firstOrNull {
                                when (it.messageEvent) {
                                    MESSAGE_CREATED.key, IMAGE_CREATED.key, CUSTOM_MESSAGE_CREATED.key -> true
                                    else -> false
                                }
                            }?.createdAt
                            val list = result.data.results.filter { chatMessage ->
                                return@filter !isMessageModerated(chatMessage)
                            }.map {
                                val pubnubChatEvent =
                                    PubnubChatEvent(
                                        it.messageEvent ?: MESSAGE_CREATED.key,
                                        it,
                                        it.pubnubTimeToken,
                                        url
                                    )
                                return@map processPubnubChatEvent(
                                    JsonParser.parseString(
                                        gson.toJson(
                                            pubnubChatEvent
                                        )
                                    ).asJsonObject,
                                    activeChatRoom?.channels?.chat?.get(CHAT_PROVIDER) ?: "",
                                    pubnubChatEvent.pubnubToken ?: 0L,
                                    it.reactions
                                )
                            }.filterNotNull()
                            listener?.onClientMessageEvents(this@PubnubChatMessagingClient, list)
                        } else {
                            listener?.onClientMessageEvents(this@PubnubChatMessagingClient, arrayListOf())
                        }
                    } else if (result is Result.Error) {
                        logError { "Error Loading Message : ${result.exception}" }
                    }
                    sendLoadingCompletedEvent(
                        activeChatRoom?.channels?.chat?.get(CHAT_PROVIDER) ?: ""
                    )
                }
            }
        }
    }

    fun addMessageAction(channel: String, messageTimetoken: Long, value: String) {
        pubnub.addMessageAction()
            .channel(channel)
            .messageAction(
                PNMessageAction().apply {
                    type = REACTION_CREATED
                    this.value = value
                    this.messageTimetoken = messageTimetoken
                }
            ).async { result, status ->
                if (!status.isError) {
                    val clientMessage = ClientMessage(
                        JsonObject().apply {
                            addProperty("event", ChatViewModel.EVENT_REACTION_ADDED)
                            addProperty(
                                "isOwnReaction",
                                true
                            )
                            addProperty(
                                "actionPubnubToken",
                                result?.actionTimetoken
                            )
                            addProperty(
                                "messagePubnubToken",
                                result?.messageTimetoken
                            )
                            addProperty("emojiId", result?.value)
                        },
                        channel
                    )
                    listener?.onClientMessageEvent(
                        this@PubnubChatMessagingClient,
                        clientMessage
                    )
                    logDebug { "own message action added" }
                } else {
                    status.errorData.throwable.printStackTrace()
                }
            }
    }

    fun removeMessageAction(channel: String, messageTimetoken: Long, actionTimetoken: Long) {
        pubnub.removeMessageAction()
            .channel(channel)
            .messageTimetoken(messageTimetoken)
            .actionTimetoken(actionTimetoken)
            .async { _, status ->
                if (!status.isError) {
                    logDebug { "own message action removed" }
                } else {
                    status.errorData.throwable.printStackTrace()
                }
            }
    }

    internal fun getMessageCountFromServer(
        startTimestamp: Long,
        endTimeStamp: Long = Calendar.getInstance().timeInMillis,
        liveLikeCallback: LiveLikeCallback<PubnubChatListCountResponse>
    ) {
        activeChatRoom?.let { chatRoom ->
            chatRoom.chatroomMessagesCountUrl?.let { url ->
                coroutineScope.launch {
                    val result = chatRepository.getMessageCount(
                        url,
                        since = ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(startTimestamp),
                            ZoneId.of("UTC")
                        ).isoDateTimeFormat(),
                        until = ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(endTimeStamp),
                            ZoneId.of("UTC")
                        ).isoDateTimeFormat(),
                    )
                    liveLikeCallback.processResult(result)
                }
            }
        }
    }

    /**
     * returns the message count between start and end time
     * max count can returned is 100
     **/
    internal fun getMessageCountV1(
        channel: String,
        startTimestamp: Long,
        endTimeStamp: Long = Calendar.getInstance().timeInMillis
    ): Result<Byte> {
        try {
            val pnHistoryResult = pubnub.history()
                .channel(channel)
                .start(convertToTimeToken(startTimestamp))
                .end(convertToTimeToken(endTimeStamp))
                .includeMeta(true)
                .includeTimetoken(true)
                .reverse(false)
                .sync()

            var count: Byte = 0
            pnHistoryResult?.messages?.forEach {
                if (it.meta.isJsonObject && it.meta.asJsonObject.get("content_filter")?.asString?.contains(
                        "filtered"
                    ) == false
                )
                    count++
            }
            return Result.Success(count)
        } catch (e: PubNubException) {
            return Result.Error(e)
        }
    }

    override fun subscribe(channels: List<String>) {
        channels.forEach {
            connectedChannels.add(it)
//            loadMessagesWithReactions(it)
            loadMessagesWithReactionsFromServer()
        }
        pubnub.subscribe().channels(channels).execute()
    }

    override fun unsubscribe(channels: List<String>) {
        pubnub.unsubscribe().channels(channels).execute()
        channels.forEach {
            connectedChannels.remove(it)
        }
    }

    override fun unsubscribeAll() {
        pubnub.unsubscribeAll()
    }

    private fun sendLoadingCompletedEvent(channel: String) {
        val msg = JsonObject().apply {
            addProperty("event", ChatViewModel.EVENT_LOADING_COMPLETE)
        }
        // TODO: remove one event once the default chat is merged with custom chat
        listener?.onClientMessageEvents(this, arrayListOf())
        listener?.onClientMessageEvent(
            this,
            ClientMessage(
                msg, channel,
                EpochTime(0)
            )
        )
    }

    override fun addMessagingEventListener(listener: MessagingEventListener) {
        // More than one triggerListener?
        this.listener = listener
    }

    override fun destroy() {
        coroutineScope.cancel()
        unsubscribeAll()
        pubnub.destroy()
        flushPublishedMessage(*connectedChannels.toTypedArray())
    }

    companion object {
        private const val PREF_CHAT_ROOM_MSG_RECEIVED = "pubnub message received"
    }
}
