package com.livelike.livelikesdk

import android.content.Context
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.engagementsdkapi.MixpanelAnalytics
import com.livelike.livelikesdk.chat.ChatRenderer
import com.livelike.livelikesdk.chat.ChatViewModel
import com.livelike.livelikesdk.chat.toChatQueue
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.services.messaging.proxies.logAnalytics
import com.livelike.livelikesdk.services.messaging.proxies.syncTo
import com.livelike.livelikesdk.services.messaging.proxies.withPreloader
import com.livelike.livelikesdk.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.livelikesdk.services.messaging.sendbird.SendbirdMessagingClient
import com.livelike.livelikesdk.services.network.EngagementDataClientImpl
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getNickename
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getSessionId
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.setNickname
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.setSessionId
import com.livelike.livelikesdk.utils.logVerbose
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import com.livelike.livelikesdk.widget.asWidgetManager
import com.livelike.livelikesdk.widget.viewModel.WidgetContainerViewModel

internal class ContentSession(
    sdkConfiguration: Stream<EngagementSDK.SdkConfiguration>,
    private val applicationContext: Context,
    private val programId: String,
    private val currentPlayheadTime: () -> EpochTime
) : LiveLikeContentSession {
    override lateinit var analyticService: AnalyticsService
    private val llDataClient = EngagementDataClientImpl()

    override val chatViewModel: ChatViewModel by lazy { ChatViewModel() }
    private var chatClient: MessagingClient? = null

    private var widgetClient: MessagingClient? = null
    private val currentWidgetViewStream = SubscriptionManager<SpecifiedWidgetView?>()
    private val widgetContainer = WidgetContainerViewModel(currentWidgetViewStream)

    override var currentUser: Stream<LiveLikeUser> = SubscriptionManager()
    private var user: LiveLikeUser? = null

    init {
        currentUser.subscribe(javaClass) { user = it }
        sdkConfiguration.subscribe(javaClass.simpleName) {
            it?.let { configuration ->
                analyticService = MixpanelAnalytics(applicationContext, configuration.mixpanelToken, programId)
                analyticService.trackConfiguration(configuration.name ?: "")

                getUser(configuration.sessionsUrl)

                if (programId.isNotEmpty()) {
                    llDataClient.getProgramData(BuildConfig.CONFIG_URL.plus("programs/$programId")) { program ->
                        if (program !== null) {
                            initializeWidgetMessaging(program.subscribeChannel, configuration)
                            initializeChatMessaging(program.chatChannel, configuration)
                            program.analyticsProps.forEach { map ->
                                analyticService.registerSuperAndPeopleProperty(map.key to map.value)
                            }
                            it.analyticsProps.forEach { map ->
                                analyticService.registerSuperAndPeopleProperty(map.key to map.value)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getUser(sessionUrl: String) {
        val sessionId = getSessionId()
        var nickname = getNickename()
        if (sessionId.isNotEmpty() && nickname.isNotEmpty()) {
            currentUser.onNext(LiveLikeUser(sessionId, nickname))
            analyticService.trackSession(sessionId)
            analyticService.trackUsername(nickname)
        } else {
            llDataClient.getUserData(sessionUrl) {
                nickname = getNickename() // Checking again the saved nickname as it could have changed during the web request.
                if (nickname.isNotEmpty()) {
                    it.nickname = nickname
                }
                currentUser.onNext(it)
                setSessionId(it.sessionId)
                setNickname(it.nickname)
                analyticService.trackSession(it.sessionId)
                analyticService.trackUsername(it.nickname)
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

    private fun initializeWidgetMessaging(subscribeChannel: String, config: EngagementSDK.SdkConfiguration) {
        analyticService.trackLastWidgetStatus(true)
        widgetClient =
            PubnubMessagingClient(config.pubNubKey)
                .logAnalytics(analyticService)
                .withPreloader(applicationContext)
                .syncTo(currentPlayheadTime)
                .asWidgetManager(llDataClient, currentWidgetViewStream, applicationContext, analyticService, config)
                .apply {
                    subscribe(hashSetOf(subscribeChannel).toList())
                }
    }

    // ///// Chat ///////

    override var chatRenderer: ChatRenderer? = null

    private fun initializeChatMessaging(chatChannel: String, config: EngagementSDK.SdkConfiguration) {
        analyticService.trackLastChatStatus(true)
        chatClient =
            SendbirdMessagingClient(config.sendBirdAppId, applicationContext, analyticService, currentUser)
                .syncTo(currentPlayheadTime, 86400000L) // Messages are valid 24 hours
                .toChatQueue()
                .apply {
                    subscribe(listOf(chatChannel))
                    this.renderer = chatRenderer
                    chatViewModel.chatListener = this
                }
    }

    override fun setChatNickname(nickname: String) {
        setNickname(nickname)
        user?.apply {
            this.nickname = nickname
            currentUser.onNext(this)
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
        analyticService.trackLastChatStatus(true)
        analyticService.trackLastWidgetStatus(true)
    }

    override fun close() {
        logVerbose { "Closing the Session" }
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
