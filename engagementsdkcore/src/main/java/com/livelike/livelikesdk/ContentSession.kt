package com.livelike.livelikesdk

import android.content.Context
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.ChatRenderer
import com.livelike.engagementsdkapi.ChatViewModel
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.engagementsdkapi.Stream
import com.livelike.livelikesdk.chat.toChatQueue
import com.livelike.livelikesdk.services.analytics.MixpanelAnalytics
import com.livelike.livelikesdk.services.messaging.MessagingClient
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

    private lateinit var analyticService: MixpanelAnalytics
    private val llDataClient = EngagementDataClientImpl()

    override val chatViewModel: ChatViewModel = ChatViewModel()
    private var chatClient: MessagingClient? = null

    private var widgetClient: MessagingClient? = null
    private val currentWidgetViewStream = SubscriptionManager<SpecifiedWidgetView?>()
    private val widgetContainer = WidgetContainerViewModel(currentWidgetViewStream)

    init {
        sdkConfiguration.subscribe(javaClass.simpleName) {
            it?.let { configuration ->
                analyticService = MixpanelAnalytics(applicationContext, configuration.mixpanelToken, programId)
                analyticService.trackConfiguration(configuration.name)

                getUser(configuration.sessionsUrl)

                if (programId.isNotEmpty()) {
                    llDataClient.getProgramData(BuildConfig.CONFIG_URL.plus("programs/$programId")) { program ->
                        if (program !== null) {
                            initializeWidgetMessaging(program.subscribeChannel, configuration)
                            initializeChatMessaging(program.chatChannel, configuration)
                        }
                    }
                }
            }
        }
    }

    override var currentUser: LiveLikeUser? = null

    private fun getUser(sessionUrl: String) {
        val sessionId = getSessionId()
        val username = getNickename()
        if (sessionId.isNotEmpty() && username.isNotEmpty()) {
            currentUser = LiveLikeUser(sessionId, username)
            analyticService.trackSession(sessionId)
            analyticService.trackUsername(username)
        } else {
            llDataClient.getUserData(sessionUrl) {
                currentUser = it
                setSessionId(it.sessionId)
                setNickname(it.userName)
                analyticService.trackSession(it.sessionId)
                analyticService.trackUsername(it.userName)
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
        widgetClient =
            PubnubMessagingClient(config.pubNubKey)
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
        chatClient =
            SendbirdMessagingClient(config.sendBirdAppId, applicationContext, analyticService, currentUser)
                .syncTo(currentPlayheadTime, 86400000L) // Messages are valid 24 hours
                .toChatQueue()
                .apply {
                    subscribe(listOf(chatChannel))
                    this.renderer = chatRenderer
                }
    }

    // ////// Global Session Controls ////////

    override fun pause() {
        logVerbose { "Pausing the Session" }
        widgetClient?.stop()
        chatClient?.stop()
    }

    override fun resume() {
        logVerbose { "Resuming the Session" }
        widgetClient?.resume()
        chatClient?.resume()
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
    }
}
