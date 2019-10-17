package com.livelike.engagementsdk

import android.content.Context
import android.widget.FrameLayout
import com.livelike.engagementsdk.analytics.AnalyticsSuperProperties
import com.livelike.engagementsdk.chat.ChatViewModel
import com.livelike.engagementsdk.chat.toChatQueue
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.data.models.RewardsType
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.data.repository.UserRepository
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
import com.livelike.engagementsdk.utils.logVerbose
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.asWidgetManager
import com.livelike.engagementsdk.widget.viewModel.WidgetContainerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime

internal class ContentSession(
    sdkConfiguration: Stream<EngagementSDK.SdkConfiguration>,
    private val userRepository: UserRepository,
    private val applicationContext: Context,
    private val programId: String,
    private val currentPlayheadTime: () -> EpochTime
) : LiveLikeContentSession {
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
    private var chatClient: MessagingClient? = null
    private var widgetClient: MessagingClient? = null
    private val currentWidgetViewStream = SubscriptionManager<SpecifiedWidgetView?>()
    private val widgetContainer = WidgetContainerViewModel(currentWidgetViewStream)

    private val programRepository = ProgramRepository(programId, userRepository)

    private val animationEventsStream = SubscriptionManager<ViewAnimationEvents>(false)

    private val job = SupervisorJob()
    private val contentSessionScope = CoroutineScope(Dispatchers.Default + job)

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
                            userRepository.rewardType = program.rewardsType
                            isGamificationEnabled = !program.rewardsType.equals(RewardsType.NONE.key)
                            initializeWidgetMessaging(program.subscribeChannel, configuration)
                            initializeChatMessaging(program.chatChannel, configuration)
                            program.analyticsProps.forEach { map ->
                                analyticService.registerSuperAndPeopleProperty(map.key to map.value)
                            }
                            configuration.analyticsProps.forEach { map ->
                                analyticService.registerSuperAndPeopleProperty(map.key to map.value)
                            }
                            programRepository.program = program
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
        config: EngagementSDK.SdkConfiguration
    ) {
        analyticService.trackLastWidgetStatus(true)
        widgetClient =
            PubnubMessagingClient(config.pubNubKey)
                .filter()
                .logAnalytics(analyticService)
                .withPreloader(applicationContext)
                .syncTo(currentPlayheadTime)
                .asWidgetManager(llDataClient, currentWidgetViewStream, applicationContext, widgetInterceptorStream, analyticService, config, userRepository, programRepository, animationEventsStream)
                .apply {
                    subscribe(hashSetOf(subscribeChannel).toList())
                }
    }

    // ///// Chat ///////

    private fun initializeChatMessaging(
        chatChannel: String,
        config: EngagementSDK.SdkConfiguration
    ) {
        analyticService.trackLastChatStatus(true)
        chatClient =
            SendbirdMessagingClient(
                config.sendBirdAppId,
                applicationContext,
                analyticService,
                userRepository
            )
                .syncTo(currentPlayheadTime, 86400000L) // Messages are valid 24 hours
                .toChatQueue()
                .apply {
                    subscribe(listOf(chatChannel))
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
        widgetClient?.resume()
        chatClient?.resume()
        if (isGamificationEnabled) contentSessionScope.launch { programRepository.fetchProgramRank() }
        analyticService.trackLastChatStatus(true)
        analyticService.trackLastWidgetStatus(true)
    }

    override fun close() {
        logVerbose { "Closing the Session" }
        contentSessionScope.cancel()
        chatClient?.apply {
            unsubscribeAll()
        }
        widgetClient?.apply {
            unsubscribeAll()
        }
        widgetClient?.stop()
        chatClient?.stop()
        currentWidgetViewStream.clear()
        analyticService.trackLastChatStatus(false)
        analyticService.trackLastWidgetStatus(false)
    }
}
