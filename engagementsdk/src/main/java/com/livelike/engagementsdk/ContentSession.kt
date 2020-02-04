package com.livelike.engagementsdk

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import com.livelike.engagementsdk.analytics.AnalyticsSuperProperties
import com.livelike.engagementsdk.chat.ChatRepository
import com.livelike.engagementsdk.chat.ChatViewModel
import com.livelike.engagementsdk.chat.chatreaction.ChatReactionRepository
import com.livelike.engagementsdk.chat.data.remote.ChatRoom
import com.livelike.engagementsdk.chat.toChatQueue
import com.livelike.engagementsdk.core.ServerDataValidationException
import com.livelike.engagementsdk.core.exceptionhelpers.BugsnagClient
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.data.models.RewardsType
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.data.repository.UserRepository
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.services.messaging.proxies.filter
import com.livelike.engagementsdk.services.messaging.proxies.logAnalytics
import com.livelike.engagementsdk.services.messaging.proxies.syncTo
import com.livelike.engagementsdk.services.messaging.proxies.withPreloader
import com.livelike.engagementsdk.services.messaging.pubnub.PubnubChatMessagingClient
import com.livelike.engagementsdk.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.engagementsdk.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.services.network.Result
import com.livelike.engagementsdk.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.combineLatestOnce
import com.livelike.engagementsdk.utils.logDebug
import com.livelike.engagementsdk.utils.logError
import com.livelike.engagementsdk.utils.logVerbose
import com.livelike.engagementsdk.utils.validateUuid
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.asWidgetManager
import com.livelike.engagementsdk.widget.viewModel.WidgetContainerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime

internal class ContentSession(
    sdkConfiguration: Stream<EngagementSDK.SdkConfiguration>,
    private val userRepository: UserRepository,
    private val applicationContext: Context,
    private val programId: String,
    private val currentPlayheadTime: () -> EpochTime
) : LiveLikeContentSession {

    override fun setProfilePicUrl(url: String?) {
        userRepository.setProfilePicUrl(url)
    }

    private var pubnubClientForMessageCount: PubnubChatMessagingClient? = null
    private var privateGroupPubnubClient: PubnubChatMessagingClient? = null
    private var chatRepository: ChatRepository? = null
    private var isGamificationEnabled: Boolean = false
    override var widgetInterceptor: WidgetInterceptor? = null
        set(value) {
            field = value
            widgetInterceptorStream.onNext(value)
        }

    private var widgetThemeAttributes: WidgetViewThemeAttributes? = null

    override fun setWidgetViewThemeAttribute(widgetViewThemeAttributes: WidgetViewThemeAttributes) {
        widgetThemeAttributes = widgetViewThemeAttributes
    }

    private val widgetInterceptorStream:
            Stream<WidgetInterceptor> = SubscriptionManager()
    override var analyticService: AnalyticsService =
        MockAnalyticsService(programId)
    private val llDataClient = EngagementDataClientImpl()

    val chatViewModel: ChatViewModel by lazy { ChatViewModel(analyticService, userRepository.currentUserStream, programRepository, animationEventsStream) }
    override var getActiveChatRoom: () -> String = { chatViewModel.currentChatRoom?.id ?: "" }
    private var chatClient: MessagingClient? = null
    private var widgetClient: MessagingClient? = null
    private val currentWidgetViewStream = SubscriptionManager<Pair<String, SpecifiedWidgetView?>?>()
    private val widgetContainer = WidgetContainerViewModel(currentWidgetViewStream)

    private val programRepository = ProgramRepository(programId, userRepository)

    private val animationEventsStream = SubscriptionManager<ViewAnimationEvents>(false)

    private val job = SupervisorJob()
    private val contentSessionScope = CoroutineScope(Dispatchers.Default + job)
    // TODO: I'm going to replace the original Stream by a Flow in a following PR to not have to much changes to review right now.
    private val configurationUserPairFlow = flow {
        while (sdkConfiguration.latest() == null || userRepository.currentUserStream.latest() == null) {
            delay(1000)
        }
        emit(Pair(sdkConfiguration.latest()!!, userRepository.currentUserStream.latest()!!))
    }
    private var privateChatRoomID = ""

    private var chatRoomMap = mutableMapOf<String, ChatRoom>()

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

    init {
        userRepository.currentUserStream.subscribe(this) {
            it?.let {
                analyticService.trackUsername(it.nickname)
            }
        }

        userRepository.currentUserStream.combineLatestOnce(sdkConfiguration, this.hashCode()).subscribe(this) {
            it?.let { pair ->
                val configuration = pair.second
                chatRepository = ChatRepository(
                    configuration.pubNubKey,
                    pair.first.accessToken,
                    pair.first.id,
                    analyticService,
                    configuration.pubnubPublishKey
                )
                analyticService =
                    MixpanelAnalytics(
                        applicationContext,
                        configuration.mixpanelToken,
                        programId
                    )
                widgetContainer.analyticsService = analyticService
                analyticService.trackSession(pair.first.id)
                analyticService.trackUsername(pair.first.nickname)
                analyticService.trackConfiguration(configuration.name ?: "")

                if (programId.isNotEmpty()) {
                    llDataClient.getProgramData(configuration.programDetailUrlTemplate.replace(TEMPLATE_PROGRAM_ID, programId)) { program ->
                        if (program !== null) {
                            programRepository.program = program
                            userRepository.rewardType = program.rewardsType
                            isGamificationEnabled = !program.rewardsType.equals(RewardsType.NONE.key)
                            initializeWidgetMessaging(program.subscribeChannel, configuration, pair.first.id)
                            chatViewModel.reportUrl = program.reportUrl
                            chatViewModel.stickerPackRepository = StickerPackRepository(programId, program.stickerPacksUrl)
                            chatViewModel.chatReactionRepository = ChatReactionRepository(program.reactionPacksUrl)
                            chatViewModel.chatRepository = chatRepository
                            contentSessionScope.launch { chatViewModel.chatReactionRepository?.preloadImages(applicationContext) }
                            chatViewModel.currentChatRoom = program.defaultChatRoom
                            if (privateChatRoomID.isEmpty()) initializeChatMessaging(program.defaultChatRoom?.channels?.chat?.get("pubnub"))
                            program.analyticsProps.forEach { map ->
                                analyticService.registerSuperAndPeopleProperty(map.key to map.value)
                            }
                            configuration.analyticsProps.forEach { map ->
                                analyticService.registerSuperAndPeopleProperty(map.key to map.value)
                            }
                            contentSessionScope.launch {
                                if (isGamificationEnabled) programRepository.fetchProgramRank()
                            }
                            startObservingForGamificationAnalytics(analyticService, programRepository.programGamificationProfileStream, programRepository.rewardType)
                        }
                    }
                }
            }
        }
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
        Log.v("Here", "joinChatRoom: $chatRoomId  timestamp:$timestamp")
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
                wouldInitPrivateGroupSession(channel)
                delay(500)
                privateGroupPubnubClient?.addChannelSubscription(channel, timestamp)
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
        if (privateChatRoomID == chatRoomId) return // Already in the room
        privateChatRoomID = chatRoomId

        fetchChatRoom(chatRoomId) { chatRoom ->
            val channel = chatRoom.channels.chat[CHAT_PROVIDER] ?: ""
            delay(500)
            wouldInitPrivateGroupSession(channel)
            privateGroupPubnubClient?.activeChatRoom = channel
            chatViewModel.apply {
                flushMessages()
                currentChatRoom = chatRoom
                chatLoaded = false
            }
        }
    }

    @Synchronized
    private fun wouldInitPrivateGroupSession(channel: String) {
        if (privateGroupPubnubClient == null) {
            initializeChatMessaging(channel, syncEnabled = true, privateGroupsChat = true)
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

    private fun startObservingForGamificationAnalytics(
        analyticService: AnalyticsService,
        programGamificationProfileStream: Stream<ProgramGamificationProfile>,
        rewardType: RewardsType
    ) {
        if (rewardType != RewardsType.NONE) {
            programGamificationProfileStream.subscribe(javaClass.simpleName) {
                it?.let {
                    analyticService.trackPointThisProgram(it.points)
                    if (rewardType == RewardsType.BADGES) {
                        if (it.points == 0 && it.currentBadge == null) {
                            analyticService.registerSuperProperty(
                                AnalyticsSuperProperties.TIME_LAST_BADGE_AWARD,
                                null
                            )
                            analyticService.registerSuperProperty(
                                AnalyticsSuperProperties.BADGE_LEVEL_THIS_PROGRAM,
                                0
                            )
                        } else if (it.currentBadge != null && it.newBadges?.isNotEmpty() == true) {
                            analyticService.registerSuperProperty(
                                AnalyticsSuperProperties.TIME_LAST_BADGE_AWARD,
                                ZonedDateTime.now().formatIsoZoned8601()
                            )
                            analyticService.registerSuperProperty(
                                AnalyticsSuperProperties.BADGE_LEVEL_THIS_PROGRAM,
                                it.currentBadge.level
                            )
                        }
                    }
                }
            }
        }
    }

    override fun getPlayheadTime(): EpochTime {
        return currentPlayheadTime()
    }

    override fun contentSessionId() = programId

    // ///// Widgets ///////

    override fun setWidgetContainer(widgetView: FrameLayout) {
        widgetContainer.setWidgetContainer(widgetView)
    }

    private fun initializeWidgetMessaging(
        subscribeChannel: String,
        config: EngagementSDK.SdkConfiguration,
        uuid: String
    ) {
        if (!validateUuid(uuid)) {
            logError { "Widget Initialization Failed due no uuid compliant user id received for user" }
            // Check with ben should we assume user id will always be uuid
            BugsnagClient.client?.notify(ServerDataValidationException("User id not compliant to uuid"))
            return
        }
        analyticService.trackLastWidgetStatus(true)
        widgetClient =
            PubnubMessagingClient(config.pubNubKey, uuid)
                .filter()
                .logAnalytics(analyticService)
                .withPreloader(applicationContext)
                .syncTo(currentPlayheadTime)
                .asWidgetManager(llDataClient, currentWidgetViewStream, applicationContext, widgetInterceptorStream, analyticService, config, userRepository, programRepository, animationEventsStream, widgetThemeAttributes)
                .apply {
                    subscribe(hashSetOf(subscribeChannel).toList())
                }
    }

    // ///// Chat. ///////
    @Synchronized
    private fun initializeChatMessaging(
        chatChannel: String?,
        syncEnabled: Boolean = true,
        privateGroupsChat: Boolean = false
    ) {
        if (chatChannel == null)
            return

        analyticService.trackLastChatStatus(true)
        chatClient = chatRepository?.establishChatMessagingConnection()
        if (privateGroupsChat) {
            privateGroupPubnubClient = chatClient as PubnubChatMessagingClient
        }

        if (syncEnabled) {
            chatClient =
                chatClient?.syncTo(currentPlayheadTime)
        }
        chatClient = chatClient?.toChatQueue()
            ?.apply {
                msgListener = proxyMsgListener
                // check issue here
                if (!privateGroupsChat) {
                    subscribe(listOf(chatChannel))
                }
                this.renderer = chatViewModel
                chatViewModel.chatListener = this
            }
    }

    // ////// Global Session Controls ////////

    override fun pause() {
        logVerbose { "Pausing the Session" }
        widgetClient?.stop()
        chatClient?.stop()
        pubnubClientForMessageCount?.stop()
        analyticService.trackLastChatStatus(false)
        analyticService.trackLastWidgetStatus(false)
    }

    override fun resume() {
        logVerbose { "Resuming the Session" }
        widgetClient?.start()
        chatClient?.start()
        pubnubClientForMessageCount?.start()
        if (isGamificationEnabled) contentSessionScope.launch { programRepository.fetchProgramRank() }
        analyticService.trackLastChatStatus(true)
        analyticService.trackLastWidgetStatus(true)
    }

    override fun close() {
        logVerbose { "Closing the Session" }
        contentSessionScope.cancel()
        chatClient?.run {
            destroy()
        }
        widgetClient?.run {
            destroy()
        }
        pubnubClientForMessageCount?.run {
            destroy()
        }
        currentWidgetViewStream.clear()
        analyticService.trackLastChatStatus(false)
        analyticService.trackLastWidgetStatus(false)
    }
}
