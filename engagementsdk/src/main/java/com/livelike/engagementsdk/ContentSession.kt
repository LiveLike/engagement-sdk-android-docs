package com.livelike.engagementsdk

import android.content.Context
import android.widget.FrameLayout
import com.livelike.engagementsdk.chat.services.messaging.pubnub.PubnubChatMessagingClient
import com.livelike.engagementsdk.core.ServerDataValidationException
import com.livelike.engagementsdk.core.analytics.AnalyticsSuperProperties
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.exceptionhelpers.BugsnagClient
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.core.services.messaging.proxies.filter
import com.livelike.engagementsdk.core.services.messaging.proxies.logAnalytics
import com.livelike.engagementsdk.core.services.messaging.proxies.syncTo
import com.livelike.engagementsdk.core.services.messaging.proxies.withPreloader
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.combineLatestOnce
import com.livelike.engagementsdk.core.utils.isNetworkConnected
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.core.utils.logVerbose
import com.livelike.engagementsdk.core.utils.validateUuid
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.asWidgetManager
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.engagementsdk.widget.services.network.WidgetDataClientImpl
import com.livelike.engagementsdk.widget.viewModel.WidgetContainerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime

internal class ContentSession(
    sdkConfiguration: Stream<EngagementSDK.SdkConfiguration>,
    private val userRepository: UserRepository,
    private val applicationContext: Context,
    private val programId: String,
    private val errorDelegate: ErrorDelegate? = null,
    private val currentPlayheadTime: () -> EpochTime
) : LiveLikeContentSession {

    override fun setProfilePicUrl(url: String?) {
        userRepository.setProfilePicUrl(url)
    }

    private var pubnubClientForMessageCount: PubnubChatMessagingClient? = null
    private var privateGroupPubnubClient: PubnubChatMessagingClient? = null
    private var isGamificationEnabled: Boolean = false
    override var widgetInterceptor: WidgetInterceptor? = null
        set(value) {
            field = value
            (widgetClient as? WidgetManager)?.widgetInterceptor = value
        }

    private var widgetThemeAttributes: WidgetViewThemeAttributes? = null

    override fun setWidgetViewThemeAttribute(widgetViewThemeAttributes: WidgetViewThemeAttributes) {
        widgetThemeAttributes = widgetViewThemeAttributes
    }

    override var analyticService: AnalyticsService =
        MockAnalyticsService(programId)
    private val llDataClient =
        EngagementDataClientImpl()
    private val widgetDataClient = WidgetDataClientImpl()

    private var widgetClient: MessagingClient? = null
    private val currentWidgetViewStream =
        SubscriptionManager<Pair<String, SpecifiedWidgetView?>?>()
    internal val widgetContainer = WidgetContainerViewModel(currentWidgetViewStream)

    private val programRepository =
        ProgramRepository(
            programId,
            userRepository
        )

    private val animationEventsStream =
        SubscriptionManager<ViewAnimationEvents>(
            false
        )

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

    init {
        userRepository.currentUserStream.subscribe(this) {
            it?.let {
                analyticService.trackUsername(it.nickname)
            }
        }

        userRepository.currentUserStream.combineLatestOnce(sdkConfiguration, this.hashCode()).subscribe(this) {
            it?.let { pair ->
                val configuration = pair.second
                analyticService =
                    MixpanelAnalytics(
                        applicationContext,
                        configuration.mixpanelToken,
                        programId
                    )
                logDebug { "analyticService created" }
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
//                            chatViewModel.reportUrl = program.reportUrl
//                            chatViewModel.stickerPackRepository = StickerPackRepository(programId, program.stickerPacksUrl)
//                            chatViewModel.chatReactionRepository = ChatReactionRepository(program.reactionPacksUrl)
//                            chatViewModel.chatRepository = chatRepository
//                            contentSessionScope.launch { chatViewModel.chatReactionRepository?.preloadImages(applicationContext) }
//                            if (privateChatRoomID.isEmpty()) {
//                                chatViewModel.currentChatRoom = program.defaultChatRoom
//                                initializeChatMessaging(program.defaultChatRoom?.channels?.chat?.get("pubnub"))
//                            }
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
        if (!applicationContext.isNetworkConnected()) {
            errorDelegate?.onError("Network error please create the session again")
        }
    }

//
//    @Synchronized
//    private fun wouldInitPrivateGroupSession(channel: String) {
//        if (privateGroupPubnubClient == null) {
//            initializeChatMessaging(channel, syncEnabled = true, privateGroupsChat = true)
//        }
//    }

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

    override fun setWidgetContainer(
        widgetView: FrameLayout,
        widgetViewThemeAttributes: WidgetViewThemeAttributes
    ) {
        widgetContainer.setWidgetContainer(widgetView, widgetViewThemeAttributes)
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
            PubnubMessagingClient(
                config.pubNubKey,
                uuid
            )
                .filter()
                .logAnalytics(analyticService)
                .withPreloader(applicationContext)
                .syncTo(currentPlayheadTime)
                .asWidgetManager(widgetDataClient, currentWidgetViewStream, applicationContext, widgetInterceptor, analyticService, config, userRepository, programRepository, animationEventsStream, widgetThemeAttributes)
                .apply {
                    subscribe(hashSetOf(subscribeChannel).toList())
                }
        logDebug { "initialized Widget Messaging" }
    }

    // ///// Chat. ///////
//    @Synchronized
//    private fun initializeChatMessaging(
//        chatChannel: String?,
//        syncEnabled: Boolean = true,
//        privateGroupsChat: Boolean = false
//    ) {
//        if (chatChannel == null)
//            return
//
//        analyticService.trackLastChatStatus(true)
//        chatClient = chatRepository?.establishChatMessagingConnection()
//        if (privateGroupsChat) {
//            privateGroupPubnubClient = chatClient as PubnubChatMessagingClient
//        }
//
//        if (syncEnabled) {
//            chatClient =
//                chatClient?.syncTo(currentPlayheadTime)
//        }
//        chatClient = chatClient?.toChatQueue()
//            ?.apply {
//                msgListener = proxyMsgListener
//                // check issue here
//                flushPublishedMessage(chatChannel)
//                if (!privateGroupsChat) {
//                    subscribe(listOf(chatChannel))
//                }
//                this.renderer = chatViewModel
//                chatViewModel.chatLoaded = false
//                chatViewModel.chatListener = this
//            }
//        logDebug { "initialized Chat Messaging , isPrivateGroupChat:$privateGroupsChat" }
//    }

    // ////// Global Session Controls ////////

    override fun pause() {
        logVerbose { "Pausing the Session" }
        widgetClient?.stop()
        pubnubClientForMessageCount?.stop()
        analyticService.trackLastChatStatus(false)
        analyticService.trackLastWidgetStatus(false)
    }

    override fun resume() {
        logVerbose { "Resuming the Session" }
        widgetClient?.start()
        pubnubClientForMessageCount?.start()
        if (isGamificationEnabled) contentSessionScope.launch { programRepository.fetchProgramRank() }
        analyticService.trackLastChatStatus(true)
        analyticService.trackLastWidgetStatus(true)
    }

    override fun close() {
        logVerbose { "Closing the Session" }
        contentSessionScope.cancel()

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
