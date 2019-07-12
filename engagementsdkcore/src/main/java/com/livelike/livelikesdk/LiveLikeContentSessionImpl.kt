package com.livelike.livelikesdk

import android.content.Context
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.ChatRenderer
import com.livelike.engagementsdkapi.ChatViewModel
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.livelikesdk.chat.ChatQueue
import com.livelike.livelikesdk.chat.toChatQueue
import com.livelike.livelikesdk.services.analytics.MixpanelAnalytics
import com.livelike.livelikesdk.services.messaging.proxies.syncTo
import com.livelike.livelikesdk.services.messaging.proxies.withPreloader
import com.livelike.livelikesdk.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.livelikesdk.services.messaging.sendbird.SendbirdChatClient
import com.livelike.livelikesdk.services.messaging.sendbird.SendbirdMessagingClient
import com.livelike.livelikesdk.services.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.utils.Provider
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getNickename
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getSessionId
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.setNickname
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.setSessionId
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import com.livelike.livelikesdk.widget.WidgetManager
import com.livelike.livelikesdk.widget.asWidgetManager
import com.livelike.livelikesdk.widget.viewModel.WidgetContainerViewModel

internal class LiveLikeContentSessionImpl(
    private val sdkConfiguration: Provider<LiveLikeSDK.SdkConfiguration>,
    private val applicationContext: Context,
    private val programId: String,
    private val currentPlayheadTime: () -> EpochTime
) : LiveLikeContentSession {

    override val chatViewModel: ChatViewModel = ChatViewModel()
    private val llDataClient = LiveLikeDataClientImpl()
    private var program: Program? = null
    private var widgetEventsQueue: WidgetManager? = null
    private var chatQueue: ChatQueue? = null
    private val currentWidgetViewStream = SubscriptionManager<SpecifiedWidgetView?>()
    private val widgetContainer = WidgetContainerViewModel(currentWidgetViewStream)
    private var analyticService: MixpanelAnalytics? = null

    override fun setWidgetContainer(widgetView: FrameLayout) {
        widgetContainer.setWidgetContainer(widgetView)
    }

    init {
        sdkConfiguration.subscribe { configuration ->
            analyticService = MixpanelAnalytics(applicationContext, configuration.mixpanelToken, programId)
            analyticService?.trackConfiguration(configuration.name)

            getUser()

            if (programId.isNotEmpty()) {
                llDataClient.getLiveLikeProgramData(BuildConfig.PROGRAM_URL.plus(programId)) {
                    if (it !== null) {
                        program = it
                        initializeWidgetMessaging(it)
                        initializeChatMessaging(it)
                    }
                }
            }
        }
    }

    private fun getUser() {
        val sessionId = getSessionId()
        val username = getNickename()
        if (!sessionId.isEmpty() && !username.isEmpty()) {
            currentUser = LiveLikeUser(sessionId, username)
            analyticService?.trackSession(sessionId)
            analyticService?.trackUsername(username)
        } else {
            sdkConfiguration.subscribe { configuration ->
                llDataClient.getLiveLikeUserData(configuration.sessionsUrl) {
                    currentUser = it
                    setSessionId(it.sessionId)
                    setNickname(it.userName)
                    analyticService?.trackSession(it.sessionId)
                    analyticService?.trackUsername(it.userName)
                }
            }
        }
    }

    override var currentUser: LiveLikeUser? = null

    override var chatRenderer: ChatRenderer? = null
        set(renderer) {
            field = renderer
            chatQueue?.renderer = chatRenderer
        }

    override fun getPlayheadTime(): EpochTime {
        return currentPlayheadTime()
    }

    override fun contentSessionId() = programId
    private fun initializeWidgetMessaging(program: Program) {
        widgetEventsQueue?.apply {
            unsubscribeAll()
        }
        sdkConfiguration.subscribe {
            val widgetQueue =
                analyticService?.let { it1 ->
                    PubnubMessagingClient(it.pubNubKey)
                        .withPreloader(applicationContext)
                        .syncTo(currentPlayheadTime)
                        .asWidgetManager(llDataClient, currentWidgetViewStream, applicationContext, it1, it)
                }
            widgetQueue?.subscribe(hashSetOf(program.subscribeChannel).toList())
            this.widgetEventsQueue = widgetQueue
        }
    }

    private fun initializeChatMessaging(program: Program) {
        chatQueue?.apply {
            unsubscribeAll()
        }

        sdkConfiguration.subscribe {
            val sendBirdMessagingClient = SendbirdMessagingClient(it.sendBirdAppId, applicationContext, currentUser)
            // validEventBufferMs for chat is currently 24 hours
            val chatClient = SendbirdChatClient()
            chatClient.messageHandler = sendBirdMessagingClient
            val chatQueue =
                analyticService?.let { it1 ->
                    sendBirdMessagingClient
                        .syncTo(currentPlayheadTime, 86400000L)
                        .toChatQueue(chatClient, it1)
                }
            chatQueue?.subscribe(listOf(program.chatChannel))
            chatQueue?.renderer = chatRenderer
            this.chatQueue = chatQueue
        }
    }

    override fun pause() {
        chatQueue?.toggleEmission(true)
    }

    override fun resume() {
        chatQueue?.toggleEmission(false)
    }

    override fun close() {
        chatQueue?.apply {
            unsubscribeAll()
        }
        widgetEventsQueue?.apply {
            unsubscribeAll()
        }
        currentWidgetViewStream.clear()
    }
}