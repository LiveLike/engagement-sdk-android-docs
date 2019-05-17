package com.livelike.livelikesdk

import android.content.Context
import com.livelike.engagementsdkapi.ChatRenderer
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.engagementsdkapi.Stream
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.chat.ChatQueue
import com.livelike.livelikesdk.chat.toChatQueue
import com.livelike.livelikesdk.services.analytics.analyticService
import com.livelike.livelikesdk.services.messaging.proxies.syncTo
import com.livelike.livelikesdk.services.messaging.proxies.withPreloader
import com.livelike.livelikesdk.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.livelikesdk.services.messaging.sendbird.SendbirdChatClient
import com.livelike.livelikesdk.services.messaging.sendbird.SendbirdMessagingClient
import com.livelike.livelikesdk.services.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getNickename
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getSessionId
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.setNickname
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.setSessionId
import com.livelike.livelikesdk.widget.WidgetManager
import com.livelike.livelikesdk.widget.asWidgetManager
import java.util.concurrent.ConcurrentHashMap

internal class LiveLikeContentSessionImpl(
    private val sdkConfiguration: Provider<LiveLikeSDK.SdkConfiguration>,
    private val applicationContext: Context
) : LiveLikeContentSession {
    override var programUrl: String = ""
        set(value) {
            if (field != value) {
                field = value
                if (programUrl.isNotEmpty()) {
                    llDataClient.getLiveLikeProgramData(value) {
                        if (it !== null) {
                            program = it
//                        currentWidgetInfosStream.clear()
                            initializeWidgetMessaging(it)
                            initializeChatMessaging(it)
                        }
                    }
                }
            }
        }
    override var currentPlayheadTime: () -> EpochTime = { EpochTime(0) }

    private val llDataClient = LiveLikeDataClientImpl()
    private var program: Program? = null
    private var widgetEventsQueue: WidgetManager? = null
    private var chatQueue: ChatQueue? = null

    override val currentWidgetInfosStream = SubscriptionManager<WidgetInfos?>()

    init {
        getUser()
    }

    private fun getUser() {
        val sessionId = getSessionId()
        val username = getNickename()
        if (!sessionId.isEmpty() && !username.isEmpty()) {
            currentUser = LiveLikeUser(sessionId, username)
            analyticService.trackSession(sessionId)
            analyticService.trackUsername(username)
        } else {
            sdkConfiguration.subscribe { configuration ->
                llDataClient.getLiveLikeUserData(configuration.sessionsUrl) {
                    currentUser = it
                    setSessionId(it.sessionId)
                    setNickname(it.userName)
                    analyticService.trackSession(it.sessionId)
                    analyticService.trackUsername(it.userName)
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

    override fun contentSessionId() = program?.contentId ?: ""
    private fun initializeWidgetMessaging(program: Program) {
        widgetEventsQueue?.apply {
            unsubscribeAll()
        }
        sdkConfiguration.subscribe {
            val widgetQueue =
                PubnubMessagingClient(it.pubNubKey)
                    .withPreloader(applicationContext)
//                    .syncTo(currentPlayheadTime)
                    .asWidgetManager(llDataClient, currentWidgetInfosStream)
            widgetQueue.subscribe(hashSetOf(program.subscribeChannel).toList())
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
                sendBirdMessagingClient
                    .syncTo(currentPlayheadTime, 86400000L)
                    .toChatQueue(chatClient)
            chatQueue.subscribe(listOf(program.chatChannel))
            chatQueue.renderer = chatRenderer
            this.chatQueue = chatQueue
        }
    }

    override fun pause() {
        chatQueue?.toggleEmission(true)
    }

    override fun resume() {
        chatQueue?.toggleEmission(false)
    }

    override fun clearChatHistory() {
        //  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearFeedbackQueue() {
        //  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        //   TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

internal interface Provider<T> {
    fun subscribe(ready: (T) -> Unit)
}

internal class SubscriptionManager<T> : Stream<T> {
    private val observerMap = ConcurrentHashMap<Any, (T?) -> Unit>()
    private var currentData: T? = null

    override fun onNext(data1: T?) {
        observerMap.forEach {
            it.value.invoke(data1)
        }
        currentData = data1
    }

    override fun subscribe(key: Any, observer: (T?) -> Unit) {
        observerMap[key] = observer
        observer.invoke(currentData)
    }

    override fun unsubscribe(key: Any) {
        observerMap.remove(key)
    }

    override fun clear() {
        currentData = null
        onNext(null)
        observerMap.clear()
    }
}