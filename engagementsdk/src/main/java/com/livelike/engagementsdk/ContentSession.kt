package com.livelike.engagementsdk

import android.content.Context
import android.widget.FrameLayout
import com.google.gson.JsonParseException
import com.livelike.engagementsdk.chat.ChatSession
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.analytics.AnalyticsSuperProperties
import com.livelike.engagementsdk.core.data.models.LeaderBoardForClient
import com.livelike.engagementsdk.core.data.models.LeaderboardClient
import com.livelike.engagementsdk.core.data.models.LeaderboardPlacement
import com.livelike.engagementsdk.core.data.models.RewardItem
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.core.services.messaging.proxies.filter
import com.livelike.engagementsdk.core.services.messaging.proxies.logAnalytics
import com.livelike.engagementsdk.core.services.messaging.proxies.syncTo
import com.livelike.engagementsdk.core.services.messaging.proxies.withPreloader
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.combineLatestOnce
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.isNetworkConnected
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.core.utils.logVerbose
import com.livelike.engagementsdk.core.utils.validateUuid
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.asWidgetManager
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.data.models.PublishedWidgetListResponse
import com.livelike.engagementsdk.widget.domain.LeaderBoardDelegate
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
import java.io.IOException

internal class ContentSession(
    clientId: String,
    sdkConfiguration: Stream<EngagementSDK.SdkConfiguration>,
    private val userRepository: UserRepository,
    private val applicationContext: Context,
    private val programId: String,
    internal val analyticServiceStream: Stream<AnalyticsService>,
    private val errorDelegate: ErrorDelegate? = null,
    private val currentPlayheadTime: () -> EpochTime
) : LiveLikeContentSession {

    override fun setProfilePicUrl(url: String?) {
        userRepository.setProfilePicUrl(url)
    }

    override var chatSession: ChatSession = ChatSession(
        clientId,
        sdkConfiguration,
        userRepository,
        applicationContext,
        true,
        analyticServiceStream,
        errorDelegate, currentPlayheadTime
    )

    override var contentSessionleaderBoardDelegate: LeaderBoardDelegate? = null
        set(value) {
            field = value
            userRepository.leaderBoardDelegate = value
        }

    internal var engagementSDK: EngagementSDK? = null
    private var isGamificationEnabled: Boolean = false
    override var widgetInterceptor: WidgetInterceptor? = null
        set(value) {
            field = value
            (widgetClient as? WidgetManager)?.widgetInterceptor = value
        }

    private var widgetThemeAttributes: WidgetViewThemeAttributes? = null
    private var publishedWidgetListResponse: PublishedWidgetListResponse? = null
    internal var isSetSessionCalled = false

    override fun setWidgetViewThemeAttribute(widgetViewThemeAttributes: WidgetViewThemeAttributes) {
        widgetThemeAttributes = widgetViewThemeAttributes
    }

    override fun getPublishedWidgets(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<LiveLikeWidget>>
    ) {
        uiScope.launch {
            val defaultUrl =
                "${BuildConfig.CONFIG_URL}programs/$programId/widgets/?status=published&ordering=recent"
            val url = when (liveLikePagination) {
                LiveLikePagination.FIRST -> defaultUrl
                LiveLikePagination.NEXT -> publishedWidgetListResponse?.next
                LiveLikePagination.PREVIOUS -> publishedWidgetListResponse?.previous
            }
            try {
                if (url == null) {
                    liveLikeCallback.onResponse(null, null)
                } else {
                    val jsonObject = widgetDataClient.getAllPublishedWidgets(url)
                    publishedWidgetListResponse =
                        gson.fromJson(
                            jsonObject.toString(),
                            PublishedWidgetListResponse::class.java
                        )
                    publishedWidgetListResponse?.results?.filter {
                        it?.let {
                            var widgetType = it.kind
                            widgetType = if (widgetType?.contains("follow-up") == true) {
                                "$widgetType-updated"
                            } else {
                                "$widgetType-created"
                            }
                            return@filter WidgetType.fromString(widgetType) != null
                        }
                        return@filter false
                    }
                        .let {
                            liveLikeCallback.onResponse(
                                it
                                , null
                            )
                        }
                }
            } catch (e: JsonParseException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            } catch (e: IOException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            }

        }
    }

    override fun getRewardItems(): List<RewardItem> {
        return programRepository.program?.rewardItems ?: listOf()
    }

    override fun getLeaderboardClients(
        leaderBoardId: List<String>,
        liveLikeCallback: LiveLikeCallback<LeaderboardClient>
    ) {
        engagementSDK?.getLeaderboardClients(leaderBoardId, liveLikeCallback)

        engagementSDK?.leaderBoardDelegate =
            object :
                LeaderBoardDelegate {
                override fun leaderBoard(
                    leaderBoard: LeaderBoardForClient,
                    currentUserPlacementDidChange: LeaderboardPlacement
                ) {
                    contentSessionleaderBoardDelegate?.leaderBoard(
                        leaderBoard,
                        currentUserPlacementDidChange
                    )
                }

            }
    }


    private val llDataClient =
        EngagementDataClientImpl()
    private val widgetDataClient = WidgetDataClientImpl()

    private var widgetClient: MessagingClient? = null
    private val currentWidgetViewStream =
        SubscriptionManager<Pair<String, SpecifiedWidgetView?>?>()
    internal val widgetContainer = WidgetContainerViewModel(currentWidgetViewStream)
    override val widgetStream = SubscriptionManager<LiveLikeWidget>()
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
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    // TODO: I'm going to replace the original Stream by a Flow in a following PR to not have to much changes to review right now.
    private val configurationUserPairFlow = flow {
        while (sdkConfiguration.latest() == null || userRepository.currentUserStream.latest() == null) {
            delay(1000)
        }
        emit(Pair(sdkConfiguration.latest()!!, userRepository.currentUserStream.latest()!!))
    }
    val livelikeThemeStream: Stream<LiveLikeEngagementTheme> = SubscriptionManager()

    init {
        userRepository.currentUserStream.subscribe(this) {
            it?.let {
                analyticServiceStream.latest()!!.trackUsername(it.nickname)
            }
        }
        userRepository.currentUserStream.combineLatestOnce(sdkConfiguration, this.hashCode())
            .subscribe(this) {
                it?.let { pair ->
                    val configuration = pair.second

                    logDebug { "analyticService created" }
                    widgetContainer.analyticsService = analyticServiceStream.latest()
                    analyticServiceStream.latest()!!.trackSession(pair.first.id)
                    analyticServiceStream.latest()!!.trackUsername(pair.first.nickname)
                    analyticServiceStream.latest()!!.trackConfiguration(configuration.name ?: "")

                    if (programId.isNotEmpty()) {
                        llDataClient.getProgramData(
                            configuration.programDetailUrlTemplate.replace(
                                TEMPLATE_PROGRAM_ID,
                                programId
                            )
                        ) { program, error ->
                            if (program !== null) {
                                programRepository.program = program
                                userRepository.rewardType = program.rewardsType
                                userRepository.updateRewardItemCache(program.rewardItems)
                                isGamificationEnabled =
                                    !program.rewardsType.equals(RewardsType.NONE.key)
                                initializeWidgetMessaging(
                                    program.subscribeChannel,
                                    configuration,
                                    pair.first.id
                                )
                                chatSession.enterChatRoom(program.defaultChatRoom?.id ?: "")

                                /* commented, since programId and programTitle doesn't need
                                * to be a part of super properties */

                                /* program.analyticsProps.forEach { map ->
                                     analyticServiceStream.latest()
                                         ?.registerSuperAndPeopleProperty(map.key to map.value)
                                 }*/
                                configuration.analyticsProps.forEach { map ->
                                    analyticServiceStream.latest()
                                        ?.registerSuperAndPeopleProperty(map.key to map.value)
                                }
                                contentSessionScope.launch {
                                    if (isGamificationEnabled) programRepository.fetchProgramRank()
                                }
                                analyticServiceStream.latest()!!.let {
                                    startObservingForGamificationAnalytics(
                                        it,
                                        programRepository.programGamificationProfileStream,
                                        programRepository.rewardType
                                    )
                                }
                            } else if (error != null) {
                                errorDelegate?.onError(error)
                            } else {
                                errorDelegate?.onError("Invalid Error")
                            }
                        }
                    }
                }
            }
        if (!applicationContext.isNetworkConnected()) {
            errorDelegate?.onError("Network error please create the session again")
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
        widgetContainer.isLayoutTransitionEnabled = applicationContext.resources.getBoolean(R.bool.livelike_widget_component_layout_transition_enabled)
        widgetContainer.setWidgetContainer(widgetView, widgetViewThemeAttributes)
    }

    private fun initializeWidgetMessaging(
        subscribeChannel: String,
        config: EngagementSDK.SdkConfiguration,
        uuid: String
    ) {
        if (!validateUuid(uuid)) {
            logError { "Widget Initialization Failed due no uuid compliant user id received for user" }
            return
        }
        analyticServiceStream.latest()!!.trackLastWidgetStatus(true)
        widgetClient =
            PubnubMessagingClient(
                config.pubNubKey,
                uuid
            ).filter().logAnalytics(analyticServiceStream.latest()!!)
                .withPreloader(applicationContext)
                .syncTo(currentPlayheadTime)
                .asWidgetManager(
                    widgetDataClient,
                    currentWidgetViewStream,
                    applicationContext,
                    widgetInterceptor,
                    analyticServiceStream.latest()!!,
                    config,
                    userRepository,
                    programRepository,
                    animationEventsStream,
                    widgetThemeAttributes,
                    livelikeThemeStream,
                    widgetStream
                )
                .apply {
                    subscribe(hashSetOf(subscribeChannel).toList())
                }
        logDebug { "initialized Widget Messaging" }
    }

    // ////// Global Session Controls ////////

    override fun pause() {
        logVerbose { "Pausing the Session" }
        widgetClient?.stop()
        analyticServiceStream.latest()?.trackLastChatStatus(false)
        analyticServiceStream.latest()?.trackLastWidgetStatus(false)
    }

    override fun resume() {
        logVerbose { "Resuming the Session" }
        if (!isSetSessionCalled) {
            widgetContainer.removeViews()
        } else {
            isSetSessionCalled = false
        }
        widgetClient?.start()
        if (isGamificationEnabled) contentSessionScope.launch { programRepository.fetchProgramRank() }
        analyticServiceStream.latest()?.trackLastChatStatus(true)
        analyticServiceStream.latest()?.trackLastWidgetStatus(true)
    }

    override fun close() {
        logVerbose { "Closing the Session" }
        contentSessionScope.cancel()
        uiScope.cancel()
        widgetClient?.run {
            destroy()
        }
        currentWidgetViewStream.clear()
        analyticServiceStream.latest()?.trackLastChatStatus(false)
        analyticServiceStream.latest()?.trackLastWidgetStatus(false)
    }

}
