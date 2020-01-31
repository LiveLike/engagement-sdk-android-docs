package com.livelike.engagementsdk.services.messaging.pubnub

import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.REACTION_CREATED
import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.ChatMessageReaction
import com.livelike.engagementsdk.chat.ChatViewModel
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEvent
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.IMAGE_CREATED
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.IMAGE_DELETED
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.MESSAGE_CREATED
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType.MESSAGE_DELETED
import com.livelike.engagementsdk.chat.data.remote.PubnubChatMessage
import com.livelike.engagementsdk.chat.data.remote.toPubnubChatEventType
import com.livelike.engagementsdk.chat.data.toChatMessage
import com.livelike.engagementsdk.chat.data.toPubnubChatMessage
import com.livelike.engagementsdk.parseISODateTime
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.services.messaging.Error
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.services.network.Result
import com.livelike.engagementsdk.utils.Queue
import com.livelike.engagementsdk.utils.extractStringOrEmpty
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.isoUTCDateTimeFormatter
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.addPublishedMessage
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.flushPublishedMessage
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.getPublishedMessages
import com.livelike.engagementsdk.utils.logDebug
import com.livelike.engagementsdk.utils.logError
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.PubNubException
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.enums.PNOperationType
import com.pubnub.api.enums.PNReconnectionPolicy
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.history.PNFetchMessageItem
import com.pubnub.api.models.consumer.history.PNFetchMessagesResult
import com.pubnub.api.models.consumer.history.PNHistoryResult
import com.pubnub.api.models.consumer.message_actions.PNAddMessageActionResult
import com.pubnub.api.models.consumer.message_actions.PNMessageAction
import com.pubnub.api.models.consumer.message_actions.PNRemoveMessageActionResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime

const val MAX_HISTORY_COUNT_PER_CHANNEL = 100

internal class PubnubChatMessagingClient(
    subscriberKey: String,
    authKey: String,
    uuid: String,
    private val analyticsService: AnalyticsService,
    publishKey: String? = null,
    val isDiscardOwnPublishInSubcription: Boolean = true
) : MessagingClient {

    @Volatile
    private var lastActionTimeToken: Long = 0
    private var connectedChannels: MutableSet<String> = mutableSetOf()

    private val publishQueue = Queue<Pair<String, PubnubChatEvent<PubnubChatMessage>>>()

    private val coroutineScope = MainScope()
    private var isPublishRunning = false

    var activeChatRoom = ""
        set(value) {
            field = value
            flushPublishedMessage(*connectedChannels.toTypedArray())
            subscribe(listOf(value))
        }

    fun addChannelSubscription(channel: String, startTimestamp: Long) {
        if (!connectedChannels.contains(channel)) {
            connectedChannels.add(channel)
            val endTimeStamp = Calendar.getInstance().timeInMillis
            pubnub.subscribe().channels(listOf(channel)).execute()
            getAllMessages(channel, convertToTimeToken(startTimestamp), convertToTimeToken(endTimeStamp))
        }
    }

    private fun convertToTimeToken(timestamp: Long): Long {
        return timestamp * 10000
    }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        val clientMessage = gson.fromJson(message, ChatMessage::class.java)
        val pubnubChatEvent = PubnubChatEvent(
            clientMessage.messageEvent.key, clientMessage.toPubnubChatMessage(
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(timeSinceEpoch.timeSinceEpochInMs),
                    org.threeten.bp.ZoneId.of("UTC")
                ).format(isoUTCDateTimeFormatter)
            )
        )
        publishQueue.enqueue(Pair(channel, pubnubChatEvent))
        if (isDiscardOwnPublishInSubcription) {
            addPublishedMessage(channel, pubnubChatEvent.payload.messageId)
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
                    if (publishMessageToPubnub(
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

    private suspend fun publishMessageToPubnub(
        pubnubChatEvent: PubnubChatEvent<PubnubChatMessage>,
        channel: String
    ) = suspendCoroutine<Boolean> {
        pubnub.publish()
            .message(
                pubnubChatEvent
            )
            .meta(JsonObject().apply {
                addProperty("sender_id", pubnubChatEvent.payload.senderId)
                addProperty("language", "en-us")
            })
            .channel(channel)
            .async(object : PNCallback<PNPublishResult>() {
                override fun onResponse(result: PNPublishResult?, status: PNStatus) {
                    logDebug { "pub status code: " + status?.statusCode }
                    if (!status.isError) {
                        logDebug { "pub timetoken: " + result?.timetoken!! }
                        it.resume(true)
                    } else {
                        it.resume(false)
                    }
                }
            })
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
        pubnubConfiguration.reconnectionPolicy = PNReconnectionPolicy.EXPONENTIAL
        pubnub = PubNub(pubnubConfiguration)
        val client = this

        // Extract SubscribeCallback?
        pubnub.addListener(object : PubnubSubscribeCallbackAdapter() {
            override fun status(pubnub: PubNub, status: PNStatus) {
                when (status.operation) {
                    // let's combine unsubscribe and subscribe handling for ease of use
                    PNOperationType.PNSubscribeOperation, PNOperationType.PNUnsubscribeOperation -> {
                        // note: subscribe statuses never have traditional
                        // errors, they just have categories to represent the
                        // different issues or successes that occur as part of subscribe
                        when (status.category) {
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
                                listener?.onClientMessageStatus(client, ConnectionStatus.DISCONNECTED)
                            }

                            PNStatusCategory.PNAccessDeniedCategory -> {
                                // this means that PAM does allow this client to subscribe to this
                                // channel and channel group configuration. This is another explicit error
                                listener?.onClientMessageError(client, Error("Access Denied", "Access Denied"))
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

            override fun message(pubnub: PubNub, message: PNMessageResult) {
                processPubnubChatEvent(message.message.asJsonObject.apply {
                    addProperty("pubnubToken", message.timetoken) }, message.channel, client, message.timetoken)
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
        })
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
        client: PubnubChatMessagingClient,
        timeToken: Long,
        actions: HashMap<String, HashMap<String, List<PNFetchMessageItem.Action>>>? = null
    ) {
        val event = jsonObject.extractStringOrEmpty("event").toPubnubChatEventType()
        if (event != null) {
            val pubnubChatEvent: PubnubChatEvent<PubnubChatMessage> = gson.fromJson(jsonObject,
                object : TypeToken<PubnubChatEvent<PubnubChatMessage>>() {}.type)
            var clientMessage: ClientMessage? = null
            when (event) {
                MESSAGE_CREATED, IMAGE_CREATED -> {
                    if (isDiscardOwnPublishInSubcription && getPublishedMessages(channel).contains(pubnubChatEvent.payload.messageId)) {
                        logError { "discarding as its own recently published message which is broadcasted by pubnub on that channel." }
                        clientMessage = ClientMessage(
                            JsonObject().apply {
                                addProperty("event", ChatViewModel.EVENT_MESSAGE_TIMETOKEN_UPDATED)
                                addProperty("messageId", pubnubChatEvent.payload.messageId)
                                addProperty("timetoken", timeToken)
                            },
                            channel
                        )
                        listener?.onClientMessageEvent(client, clientMessage)
                        return // discarding as its own recently published message which is broadcasted by pubnub on that channel.
                    }
                    val pdtString = pubnubChatEvent.payload.programDateTime
                    var epochTimeMs = 0L
                    pdtString?.parseISODateTime()?.let {
                        epochTimeMs = it.toInstant().toEpochMilli()
                    }

                    try {
                        clientMessage = ClientMessage(
                            gson.toJsonTree(pubnubChatEvent.payload.toChatMessage(channel, timeToken, processReactionCounts(actions), getOwnReaction(actions), event)).asJsonObject.apply {
                                addProperty("event", ChatViewModel.EVENT_NEW_MESSAGE)
                                addProperty("pubnubMessageToken", pubnubChatEvent.pubnubToken)
                            },
                            channel,
                            EpochTime(epochTimeMs)
                        )
                    } catch (ex: IllegalArgumentException) {
                        logError { ex.message }
                        return
                    }
                }
                MESSAGE_DELETED, IMAGE_DELETED -> {
                    clientMessage = ClientMessage(JsonObject().apply {
                        addProperty("event", ChatViewModel.EVENT_MESSAGE_DELETED)
                        addProperty("id", pubnubChatEvent.payload.messageId)
                    },
                        channel,
                        EpochTime(0)
                    )
                }
            }
            logError { "Received message on $channel from pubnub: ${pubnubChatEvent.payload}" }
            clientMessage?.let { listener?.onClientMessageEvent(client, clientMessage) }
        } else {
            logError { "We don't know how to handle this message" }
        }
    }

    private fun getOwnReaction(actions: java.util.HashMap<String, java.util.HashMap<String, List<PNFetchMessageItem.Action>>>?): ChatMessageReaction? {
        actions?.get(REACTION_CREATED)?.let { reactions ->
            for (value in reactions.keys) {
                reactions[value]?.forEach { action ->
                    if (action.uuid == pubnub.configuration.uuid) {
                        return ChatMessageReaction(value, action.actionTimetoken.toLongOrNull())
                    }
                }
            }
        }
        return null
    }

    private fun processReactionCounts(actions: java.util.HashMap<String, java.util.HashMap<String, List<PNFetchMessageItem.Action>>>?): MutableMap<String, Int> {
        val reactionCountMap = mutableMapOf<String, Int>()
        actions?.get(REACTION_CREATED)?.let { reactions ->
            for (value in reactions.keys) {
                reactionCountMap[value] = reactions[value]?.size ?: 0
            }
        }
        return reactionCountMap
    }

    internal fun loadMessagesWithReactions(
        channel: String,
        timeToken: Long = convertToTimeToken(Calendar.getInstance().timeInMillis),
        chatHistoyLimit: Int = com.livelike.engagementsdk.CHAT_HISTORY_LIMIT
    ) {
        pubnub.fetchMessages()
            .channels(listOf(channel))
            .includeMeta(true)
            .maximumPerChannel(chatHistoyLimit)
            .start(timeToken)
            .includeMessageActions(true)
            .async(object : PNCallback<PNFetchMessagesResult>() {
                override fun onResponse(result: PNFetchMessagesResult?, status: PNStatus) {
                    if (!status.isError && result?.channels?.get(channel)?.isEmpty() == false) {
                        result.channels?.get(channel)?.reversed()?.forEach {
                            val jsonObject = it.message.asJsonObject.apply {
                                addProperty("pubnubToken", it.timetoken)
                            }
                            processPubnubChatEvent(
                                jsonObject,
                                channel,
                                this@PubnubChatMessagingClient,
                                it.timetoken,
                                it.actions
                            )
                        }
                    }
                    sendLoadingCompletedEvent(channel)
                }
            })
    }

    @Deprecated("use loadMessagesWithReactions")
    private fun loadMessageHistoryByTimestamp(
        channel: String,
        timeToken: Long = convertToTimeToken(Calendar.getInstance().timeInMillis),
        chatHistoyLimit: Int = com.livelike.engagementsdk.CHAT_HISTORY_LIMIT
    ) {
        pubnub.history()
            .channel(channel)
            .count(chatHistoyLimit)
            .start(timeToken)
            .reverse(false)
            .async(object : PNCallback<PNHistoryResult>() {
                override fun onResponse(result: PNHistoryResult?, status: PNStatus) {
                    if (!status.isError && result?.messages?.isEmpty() == false) {
                        result.messages.forEach {
                            processPubnubChatEvent(
                                it.entry.asJsonObject,
                                channel,
                                this@PubnubChatMessagingClient,
                                it.timetoken
                            )
                        }
                    }
                    sendLoadingCompletedEvent(channel)
                }
            })
    }

    private fun getAllMessages(
        channel: String,
        startTimeToken: Long,
        endTimeToken: Long
    ) {
        pubnub.history()
            .channel(channel)
            .start(startTimeToken)
            .end(endTimeToken)
            .count(MAX_HISTORY_COUNT_PER_CHANNEL)
            .includeTimetoken(true)
            .reverse(false)
            .async(object : PNCallback<PNHistoryResult>() {
                override fun onResponse(result: PNHistoryResult?, status: PNStatus) {
                    if (!status.isError && result?.messages?.isEmpty() == false) {
                        result.messages.forEach {
                            processPubnubChatEvent(
                                it.entry.asJsonObject,
                                channel,
                                this@PubnubChatMessagingClient,
                                it.timetoken
                            )
                        }
                        if (result.messages.size >= MAX_HISTORY_COUNT_PER_CHANNEL) {
                            getAllMessages(channel, result.messages.last().timetoken, endTimeToken)
                        }
                    }
                }
            })
    }

    fun addMessageAction(channel: String, messageTimetoken: Long, value: String) {
        pubnub.addMessageAction()
            .channel(channel)
            .messageAction(PNMessageAction().apply {
                type = REACTION_CREATED
                this.value = value
                this.messageTimetoken = messageTimetoken
            }).async(object : PNCallback<PNAddMessageActionResult>() {
                override fun onResponse(result: PNAddMessageActionResult?, status: PNStatus) {
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
            })
    }

    fun removeMessageAction(channel: String, messageTimetoken: Long, actionTimetoken: Long) {
        pubnub.removeMessageAction()
            .channel(channel)
            .messageTimetoken(messageTimetoken)
            .actionTimetoken(actionTimetoken)
            .async(object : PNCallback<PNRemoveMessageActionResult>() {
            override fun onResponse(result: PNRemoveMessageActionResult?, status: PNStatus) {
                if (!status.isError) {
                    logDebug { "own message action removed" }
                } else {
                    status.errorData.throwable.printStackTrace()
                }
            }
        })
    }

    internal fun getMessageCount(
        channel: String,
        startTimestamp: Long
    ): Result<Long> {
        return try {
            val countResult = pubnub.messageCounts()
                .channels(listOf(channel))
                .channelsTimetoken(listOf(convertToTimeToken(startTimestamp)))
                .sync()
            Result.Success(countResult?.channels?.get(channel) ?: 0)
        } catch (ex: PubNubException) {
            ex.printStackTrace()
            Result.Error(ex)
        }
    }

    override fun subscribe(channels: List<String>) {
        channels.forEach {
            connectedChannels.add(it)
            loadMessagesWithReactions(it)
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
        listener?.onClientMessageEvent(
            this, ClientMessage(
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
}
