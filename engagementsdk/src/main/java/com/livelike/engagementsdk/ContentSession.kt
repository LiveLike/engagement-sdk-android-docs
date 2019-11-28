package com.livelike.engagementsdk

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.analytics.AnalyticsSuperProperties
import com.livelike.engagementsdk.chat.ChatRepository
import com.livelike.engagementsdk.chat.ChatViewModel
import com.livelike.engagementsdk.chat.toChatQueue
import com.livelike.engagementsdk.core.ServerDataValidationException
import com.livelike.engagementsdk.core.exceptionhelpers.BugsnagClient
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.data.models.RewardsType
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.data.repository.UserRepository
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.services.messaging.proxies.filter
import com.livelike.engagementsdk.services.messaging.proxies.logAnalytics
import com.livelike.engagementsdk.services.messaging.proxies.syncTo
import com.livelike.engagementsdk.services.messaging.proxies.withPreloader
import com.livelike.engagementsdk.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.engagementsdk.services.messaging.sendbird.SendbirdMessagingClient
import com.livelike.engagementsdk.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.combineLatestOnce
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.PREFERENCE_CHAT_ROOM_MEMBERSHIP
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.getSharedPreferences
import com.livelike.engagementsdk.utils.logError
import com.livelike.engagementsdk.utils.logVerbose
import com.livelike.engagementsdk.utils.validateUuid
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.asWidgetManager
import com.livelike.engagementsdk.widget.viewModel.WidgetContainerViewModel
import java.util.Calendar
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

    override fun setProfilePicUrl(url: String) {
        userRepository.setProfilePicUrl(url)
    }

    private var sendbirdMessagingClient: SendbirdMessagingClient? = null
    private var isGamificationEnabled: Boolean = false
    override var widgetInterceptor: WidgetInterceptor? = null
        set(value) {
            field = value
            widgetInterceptorStream.onNext(value)
        }
    private val widgetInterceptorStream:
            Stream<WidgetInterceptor> = SubscriptionManager()
    override var analyticService: AnalyticsService =
        MockAnalyticsService(programId)
    private val llDataClient = EngagementDataClientImpl()

    private val stickerPackRepository = StickerPackRepository(programId)
    val chatViewModel: ChatViewModel by lazy { ChatViewModel(analyticService, userRepository.currentUserStream, programRepository, animationEventsStream, stickerPackRepository) }
    override var getActiveChatRoom: () -> String = { chatViewModel.currentChatRoom }
    private var chatClient: MessagingClient? = null
    private var widgetClient: MessagingClient? = null
    private val currentWidgetViewStream = SubscriptionManager<SpecifiedWidgetView?>()
    private val widgetContainer = WidgetContainerViewModel(currentWidgetViewStream)

    private val programRepository = ProgramRepository(programId, userRepository)

    private val animationEventsStream = SubscriptionManager<ViewAnimationEvents>(false)

    private val job = SupervisorJob()
    private val contentSessionScope = CoroutineScope(Dispatchers.Default + job)
    // TODO: I'm going to replace the original Stream by a Flow in a following PR to not have to much changes to review right now.
    private val configurationFlow = flow {
        while (sdkConfiguration.latest() == null || userRepository.currentUserStream.latest() == null) {
            delay(1000)
        }
        emit(sdkConfiguration.latest()!!)
    }
    private var customChatChannel = ""

    private var chatRoomMemberships: HashMap<String, Long?>

    private var msgListener: MessageListener = object : MessageListener {
        override fun onNewMessage(chatRoom: String, message: LiveLikeChatMessage) {} }

    init {
        userRepository.currentUserStream.subscribe(javaClass) {
            it?.let {
                analyticService.trackUsername(it.nickname)
            }
        }

        userRepository.currentUserStream.combineLatestOnce(sdkConfiguration).subscribe(javaClass.simpleName) {
            it?.let { pair ->
                val configuration = pair.second
                analyticService =
                    MixpanelAnalytics(
                        applicationContext,
                        configuration.mixpanelToken,
                        programId
                    )
                analyticService.trackSession(pair.first.id)
                analyticService.trackUsername(pair.first.nickname)
                analyticService.trackConfiguration(configuration.name ?: "")

                if (programId.isNotEmpty()) {
                    llDataClient.getProgramData(BuildConfig.CONFIG_URL.plus("programs/$programId")) { program ->
                        if (program !== null) {
                            programRepository.program = program
                            userRepository.rewardType = program.rewardsType
                            isGamificationEnabled = !program.rewardsType.equals(RewardsType.NONE.key)
                            initializeWidgetMessaging(program.subscribeChannel, configuration, pair.first.id)
                            chatViewModel.currentChatRoom = program.defaultChatRoom?.channels?.chat?.get("pubnub") ?: ""
                            if (customChatChannel.isEmpty()) initializeChatMessaging(program.defaultChatRoom?.channels?.chat?.get("pubnub"), configuration, pair.first)
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
        chatRoomMemberships = gson.fromJson(getSharedPreferences().getString(
            PREFERENCE_CHAT_ROOM_MEMBERSHIP, ""), object : TypeToken<HashMap<String, Long?>>() {}.type) ?: HashMap()
    }

    override fun joinChatRoom(chatRoom: String) {
        if (chatRoomMemberships.size > 10) {
            return logError {
                "Membership count cannot be greater than 10"
            }
        }
        val chatRoom = validChatChannelName(chatRoom)
        if (!chatRoomMemberships.containsKey(chatRoom)) {
            chatRoomMemberships[chatRoom] = Calendar.getInstance().timeInMillis
            getSharedPreferences().edit()
                .putString(PREFERENCE_CHAT_ROOM_MEMBERSHIP, gson.toJson(chatRoomMemberships))
                .apply()
        }
    }

    override fun leaveChatRoom(chatRoom: String) {
        val validChatChannelName = validChatChannelName(chatRoom)
        chatRoomMemberships.remove(validChatChannelName)
        chatClient?.unsubscribe(listOf(validChatChannelName))
    }

    override fun enterChatRoom(chatRoom: String) {
        val chatRoom = validChatChannelName(chatRoom)
        if (chatRoomMemberships.containsKey(chatRoom)) {
            joinChatRoom(chatRoom)
        }
        Log.d("god", chatRoomMemberships.toString())
        if (customChatChannel == chatRoom) return
        customChatChannel = chatRoom
        contentSessionScope.launch {
            chatViewModel.flushMessages()
            chatViewModel.currentChatRoom = chatRoom
            chatViewModel.chatLoaded = false
            if (sendbirdMessagingClient == null) {
                configurationFlow.collect {
                    userRepository.currentUserStream.latest()?.let { user ->
                        initializeChatMessaging(chatRoom, it, user, syncEnabled = false, privateGroupsChat = true)
                    }
                }
            } else {
                sendbirdMessagingClient?.activeChannelRoom = chatRoom
            }
        }
    }

    private fun validChatChannelName(chatRoom: String) =
        chatRoom.toLowerCase().replace(" ", "").replace("-", "")

    override fun exitChatRoom(chatRoom: String) {
        chatClient?.unsubscribe(listOf(chatRoom))
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
                                ZonedDateTime.now().formatIsoLocal8601()
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
                .asWidgetManager(llDataClient, currentWidgetViewStream, applicationContext, widgetInterceptorStream, analyticService, config, userRepository, programRepository, animationEventsStream)
                .apply {
                    subscribe(hashSetOf(subscribeChannel).toList())
                }
    }

    // ///// Chat. ///////

    private fun initializeChatMessaging(
        chatChannel: String?,
        config: EngagementSDK.SdkConfiguration,
        user: LiveLikeUser,
        syncEnabled: Boolean = true,
        privateGroupsChat: Boolean = false
    ) {
        if (chatChannel == null)
            return

        analyticService.trackLastChatStatus(true)

        if (privateGroupsChat) {
            sendbirdMessagingClient = chatClient as SendbirdMessagingClient
            chatClient = SendbirdMessagingClient(
                config.sendBirdAppId,
                applicationContext,
                analyticService,
                userRepository,
                msgListener,
                chatRoomMemberships
            )
        } else {
            chatClient =
                ChatRepository(config.pubNubKey, user.accessToken, user.id, analyticService).establishChatMessagingConnection()
        }

        if (syncEnabled)
            chatClient =
                chatClient?.syncTo(currentPlayheadTime, 86400000L) // Messages are valid 24 hours
        chatClient = chatClient?.toChatQueue()
            ?.apply {
                if (privateGroupsChat) {
                    subscribe(chatRoomMemberships.keys.toList().filter { it != chatChannel })
                    sendbirdMessagingClient?.activeChannelRoom = chatChannel
                } else {
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
        analyticService.trackLastChatStatus(false)
        analyticService.trackLastWidgetStatus(false)
    }

    override fun resume() {
        logVerbose { "Resuming the Session" }
        widgetClient?.start()
        chatClient?.start()
        if (isGamificationEnabled) contentSessionScope.launch { programRepository.fetchProgramRank() }
        analyticService.trackLastChatStatus(true)
        analyticService.trackLastWidgetStatus(true)
    }

    override fun close() {
        logVerbose { "Closing the Session" }
        contentSessionScope.cancel()
        chatClient?.run {
            unsubscribeAll()
            stop()
            destroy()
        }
        widgetClient?.run {
            unsubscribeAll()
            stop()
            destroy()
        }
        currentWidgetViewStream.clear()
        analyticService.trackLastChatStatus(false)
        analyticService.trackLastWidgetStatus(false)
    }
}
