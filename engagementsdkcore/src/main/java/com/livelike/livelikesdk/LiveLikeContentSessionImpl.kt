package com.livelike.livelikesdk

import android.content.Context
import com.livelike.engagementsdkapi.ChatRenderer
import com.livelike.engagementsdkapi.ChatState
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.engagementsdkapi.WidgetRenderer
import com.livelike.engagementsdkapi.WidgetStream
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.analytics.analyticService
import com.livelike.livelikesdk.chat.ChatQueue
import com.livelike.livelikesdk.chat.toChatQueue
import com.livelike.livelikesdk.messaging.proxies.syncTo
import com.livelike.livelikesdk.messaging.proxies.withPreloader
import com.livelike.livelikesdk.messaging.pubnub.PubnubMessagingClient
import com.livelike.livelikesdk.messaging.sendbird.SendbirdChatClient
import com.livelike.livelikesdk.messaging.sendbird.SendbirdMessagingClient
import com.livelike.livelikesdk.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.getNickename
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.getSessionId
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.setNickname
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.setSessionId
import com.livelike.livelikesdk.widget.WidgetManager
import com.livelike.livelikesdk.widget.asWidgetManager
import java.util.concurrent.ConcurrentHashMap

internal class LiveLikeContentSessionImpl(
    override val programUrl: String,
    val currentPlayheadTime: () -> EpochTime,
    private val sdkConfiguration: Provider<LiveLikeSDK.SdkConfiguration>,
    override val applicationContext: Context
) : LiveLikeContentSession {

    private val llDataClient = LiveLikeDataClientImpl()
    private var program: Program? = null
    private var widgetQueue: WidgetManager? = null
    private var chatQueue: ChatQueue? = null

    override var chatState = ChatState()
    override var widgetState = WidgetTransientState()
    override val widgetStream = SubscriptionManager()

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

    override var widgetRenderer: WidgetRenderer? = null
        set(value) {
            field = value
            widgetQueue?.renderer = widgetRenderer
        }

    override var chatRenderer: ChatRenderer? = null
        set(renderer) {
            field = renderer
            chatQueue?.renderer = chatRenderer
        }

    init {
        llDataClient.getLiveLikeProgramData(programUrl) {
            program = it
            initializeWidgetMessaging(it)
            initializeChatMessaging(it)
        }
    }

    override fun getPlayheadTime(): EpochTime {
        return currentPlayheadTime()
    }

    override fun contentSessionId() = program?.clientId ?: ""
    private fun initializeWidgetMessaging(program: Program) {
        sdkConfiguration.subscribe {
            val widgetQueue =
                PubnubMessagingClient(it.pubNubKey)
                    .withPreloader(applicationContext)
//                    .syncTo(currentPlayheadTime)
                    .asWidgetManager(llDataClient, widgetStream, this)
            widgetQueue.unsubscribeAll()
            widgetQueue.subscribe(listOf(program.subscribeChannel))
            widgetQueue.renderer = widgetRenderer
            this.widgetQueue = widgetQueue
        }
    }

    private fun initializeChatMessaging(program: Program) {
        sdkConfiguration.subscribe {
            val sendBirdMessagingClient = SendbirdMessagingClient(it.sendBirdAppId, applicationContext, currentUser)
            // validEventBufferMs for chat is currently 24 hours
            val chatClient = SendbirdChatClient()
            chatClient.messageHandler = sendBirdMessagingClient
            val chatQueue =
                sendBirdMessagingClient
                    .syncTo(currentPlayheadTime, 86400000L)
                    .toChatQueue(chatClient)
            chatQueue.unsubscribeAll()
            chatQueue.subscribe(listOf(program.chatChannel))
            chatQueue.renderer = chatRenderer
            this.chatQueue = chatQueue
        }
    }

    override fun pause() {
        widgetQueue?.toggleEmission(true)
        chatQueue?.toggleEmission(true)
    }

    override fun resume() {
        widgetQueue?.toggleEmission(false)
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

internal class SubscriptionManager : WidgetStream {
    private val widgetMap = ConcurrentHashMap<Any, (String?) -> Unit>()

    override fun onNext(view: String?) {
        widgetMap.forEach {
            it.value.invoke(view)
        }
    }

    override fun subscribe(key: Any, observer: (String?) -> Unit) {
        widgetMap[key] = observer
    }

    override fun unsubscribe(key: Any) {
        widgetMap.remove(key)
    }

    override fun clear() {
        widgetMap.clear()
    }
}