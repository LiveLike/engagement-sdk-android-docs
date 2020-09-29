package com.livelike.engagementsdk

import android.content.Context
import android.widget.FrameLayout
import com.google.gson.JsonParseException
import com.livelike.engagementsdk.chat.ChatSession
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.chat.services.messaging.pubnub.PubnubChatMessagingClient
import com.livelike.engagementsdk.core.analytics.AnalyticsSuperProperties
import com.livelike.engagementsdk.core.data.models.LeaderBoardForClient
import com.livelike.engagementsdk.core.data.models.LeaderBoardResource
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
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.threeten.bp.ZonedDateTime
import java.io.IOException

internal class ContentSession(
    clientId: String,
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

    override var chatSession: ChatSession = ChatSession(
        clientId,
        sdkConfiguration,
        userRepository,
        applicationContext,
        true,
        errorDelegate, currentPlayheadTime
    )

    override var leaderBoardDelegate: LeaderBoardDelegate? = null
        set(value) {
            field = value
            userRepository.leaderBoardDelegate = value
        }

    private var pubnubClientForMessageCount: PubnubChatMessagingClient? = null
    private var privateGroupPubnubClient: PubnubChatMessagingClient? = null
    private var engagementSDK: EngagementSDK? = null
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
        liveLikeCallback: LiveLikeCallback<List<LiveLikeWidget?>>
    ) {
        contentSessionScope.launch {
            val defaultUrl =
                "${BuildConfig.CONFIG_URL}programs/$programId/widgets/?status=published&ordering=recent"
            val url = when (liveLikePagination) {
                LiveLikePagination.FIRST -> defaultUrl
                LiveLikePagination.NEXT -> publishedWidgetListResponse?.next ?: defaultUrl
                LiveLikePagination.PREVIOUS -> publishedWidgetListResponse?.previous ?: defaultUrl
            }
            try {
                val jsonObject = widgetDataClient.getAllPublishedWidgets(url)
                publishedWidgetListResponse =
                    gson.fromJson(
                        jsonObject.toString(),
                        PublishedWidgetListResponse::class.java
                    )
                publishedWidgetListResponse?.results?.let {
                    liveLikeCallback.onResponse(it, null)
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

        userRepository.currentUserStream.subscribe(this) {
            it?.let {
                userRepository.currentUserStream.subscribe(this) { user ->
                    userRepository.currentUserStream.unsubscribe(this)
                    engagementSDK?.configurationStream?.unsubscribe(this)
                    CoroutineScope(Dispatchers.IO).launch {

                        val leaderBoardUrlTemplate = "leaderboard_detail_url_template\""
                        val job = ArrayList<Job>()
                        for (i in 1 until leaderBoardId.size - 1) {
                            job.add(launch {
                                val url = leaderBoardUrlTemplate?.replace(
                                    TEMPLATE_LEADER_BOARD_ID,
                                    leaderBoardId.get(i)
                                )
                                val result = llDataClient.remoteCall<LeaderBoardResource>(
                                    url,
                                    requestType = RequestType.GET,
                                    accessToken = null
                                )
                                if (result is Result.Success) {
                                    user?.let { user ->
                                        val result2 = engagementSDK?.getLeaderBoardEntry(
                                            engagementSDK?.configurationStream?.latest()!!,
                                            result.data.id,
                                            user.id
                                        )
                                        if (result2 is Result.Success) {
                                            leaderBoardDelegate?.leaderBoard(
                                                LeaderBoardForClient(
                                                    result.data.id,
                                                    result.data.name,
                                                    result.data.rewardItem
                                                ), LeaderboardPlacement(
                                                    result2.data.rank
                                                    ,
                                                    result2.data.percentile_rank.toString(),
                                                    result2.data.score
                                                )
                                            )
                                            liveLikeCallback.onResponse(
                                                LeaderboardClient(
                                                    result.data.id,
                                                    result.data.name,
                                                    result.data.rewardItem,
                                                    LeaderboardPlacement(
                                                        result2.data.rank
                                                        ,
                                                        result2.data.percentile_rank.toString(),
                                                        result2.data.score
                                                    ),
                                                    leaderBoardDelegate!!
                                                )
                                                , null
                                            )
                                        } else if (result2 is Result.Error) {
                                            leaderBoardDelegate?.leaderBoard(
                                                LeaderBoardForClient(
                                                    result.data.id,
                                                    result.data.name,
                                                    result.data.rewardItem
                                                ),
                                                LeaderboardPlacement(0, " ", 0)
                                            )

                                        }
//
                                    }

                                } else if (result is Result.Error) {
                                    liveLikeCallback.onResponse(null, result.exception.message)
                                }
                            })


                        }

                        job.joinAll()
                    }
                }
            }
        }

    }


    override var analyticService: AnalyticsService =
        MockAnalyticsService(clientId)
    private val llDataClient =
        EngagementDataClientImpl()
    private val widgetDataClient = WidgetDataClientImpl()

    private var widgetClient: MessagingClient? = null
    private val currentWidgetViewStream =
        SubscriptionManager<Pair<String, SpecifiedWidgetView?>?>()
    internal val widgetContainer = WidgetContainerViewModel(currentWidgetViewStream)
    val widgetStream = SubscriptionManager<LiveLikeWidget>()
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
    val livelikeThemeStream: Stream<LiveLikeEngagementTheme> = SubscriptionManager()

    init {
        userRepository.currentUserStream.subscribe(this) {
            it?.let {
                analyticService.trackUsername(it.nickname)
            }
        }
        userRepository.currentUserStream.combineLatestOnce(sdkConfiguration, this.hashCode())
            .subscribe(this) {
                it?.let { pair ->
                    val configuration = pair.second
                    analyticService =
                        MixpanelAnalytics(
                            applicationContext,
                            configuration.mixpanelToken,
                            configuration.clientId
                        )
                    logDebug { "analyticService created" }
                    widgetContainer.analyticsService = analyticService
                    analyticService.trackSession(pair.first.id)
                    analyticService.trackUsername(pair.first.nickname)
                    analyticService.trackConfiguration(configuration.name ?: "")

                    if (programId.isNotEmpty()) {
                        llDataClient.getProgramData(
                            configuration.programDetailUrlTemplate.replace(
                                TEMPLATE_PROGRAM_ID,
                                programId
                            )
                        ) { program ->
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
                                program.analyticsProps.forEach { map ->
                                    analyticService.registerSuperAndPeopleProperty(map.key to map.value)
                                }
                                configuration.analyticsProps.forEach { map ->
                                    analyticService.registerSuperAndPeopleProperty(map.key to map.value)
                                }
                                contentSessionScope.launch {
                                    if (isGamificationEnabled) programRepository.fetchProgramRank()
                                }
                                startObservingForGamificationAnalytics(
                                    analyticService,
                                    programRepository.programGamificationProfileStream,
                                    programRepository.rewardType
                                )
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
        analyticService.trackLastWidgetStatus(true)
        widgetClient =
            PubnubMessagingClient(
                config.pubNubKey,
                uuid
            ).filter().logAnalytics(analyticService).withPreloader(applicationContext)
                .syncTo(currentPlayheadTime)
                .asWidgetManager(
                    widgetDataClient,
                    currentWidgetViewStream,
                    applicationContext,
                    widgetInterceptor,
                    analyticService,
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
        pubnubClientForMessageCount?.stop()
        analyticService.trackLastChatStatus(false)
        analyticService.trackLastWidgetStatus(false)
    }

    override fun resume() {
        logVerbose { "Resuming the Session" }
        if (!isSetSessionCalled) {
            widgetContainer.removeViews()
        } else {
            isSetSessionCalled = false
        }
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
