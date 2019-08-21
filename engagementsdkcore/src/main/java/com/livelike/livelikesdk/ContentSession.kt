package com.livelike.livelikesdk

import android.content.Context
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.engagementsdkapi.MixpanelAnalytics
import com.livelike.engagementsdkapi.MockAnalyticsService
import com.livelike.livelikesdk.chat.ChatRenderer
import com.livelike.livelikesdk.chat.ChatViewModel
import com.livelike.livelikesdk.chat.toChatQueue
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.services.messaging.proxies.WidgetInterceptor
import com.livelike.livelikesdk.services.messaging.proxies.gamify
import com.livelike.livelikesdk.services.messaging.proxies.integratorDeferredClient
import com.livelike.livelikesdk.services.messaging.proxies.logAnalytics
import com.livelike.livelikesdk.services.messaging.proxies.syncTo
import com.livelike.livelikesdk.services.messaging.proxies.withPreloader
import com.livelike.livelikesdk.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.livelikesdk.services.messaging.sendbird.SendbirdMessagingClient
import com.livelike.livelikesdk.services.network.EngagementDataClientImpl
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.setNickname
import com.livelike.livelikesdk.utils.logVerbose
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import com.livelike.livelikesdk.widget.asWidgetManager
import com.livelike.livelikesdk.widget.viewModel.WidgetContainerViewModel

internal class ContentSession(
    sdkConfiguration: Stream<EngagementSDK.SdkConfiguration>,
    private val currentUser: Stream<LiveLikeUser>,
    private val applicationContext: Context,
    private val programId: String,
    private val currentPlayheadTime: () -> EpochTime,
    override val widgetInterceptor: WidgetInterceptor? = null
) : LiveLikeContentSession {
    override var analyticService: AnalyticsService = MockAnalyticsService()
    private val llDataClient = EngagementDataClientImpl()

    override val chatViewModel: ChatViewModel by lazy { ChatViewModel() }
    private var chatClient: MessagingClient? = null

    private var widgetClient: MessagingClient? = null
    private val currentWidgetViewStream = SubscriptionManager<SpecifiedWidgetView?>()
    private val widgetContainer = WidgetContainerViewModel(currentWidgetViewStream)

    private var user: LiveLikeUser? = null

    init {
        currentUser.subscribe(javaClass) {
            user = it
            it?.let {
                analyticService.trackSession(it.sessionId)
                analyticService.trackUsername(it.nickname)
            }
        }
        sdkConfiguration.subscribe(javaClass.simpleName) {
            it?.let { configuration ->
                analyticService = MixpanelAnalytics(applicationContext, configuration.mixpanelToken, programId)
                analyticService.trackConfiguration(configuration.name ?: "")

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

    override fun getCurrentUserStream(): Stream<LiveLikeUser> {
        return currentUser
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
                .integratorDeferredClient(widgetInterceptor)
                .gamify()
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
