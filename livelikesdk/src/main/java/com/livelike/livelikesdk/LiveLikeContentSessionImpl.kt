package com.livelike.livelikesdk

import android.content.Context
import com.livelike.livelikesdk.analytics.InteractionLogger
import com.livelike.livelikesdk.analytics.InteractionSession
import com.livelike.livelikesdk.chat.ChatQueue
import com.livelike.livelikesdk.chat.ChatRenderer
import com.livelike.livelikesdk.chat.toChatQueue
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.proxies.syncTo
import com.livelike.livelikesdk.messaging.pubnub.PubnubMessagingClient
import com.livelike.livelikesdk.messaging.sendbird.SendbirdChatClient
import com.livelike.livelikesdk.messaging.sendbird.SendbirdMessagingClient
import com.livelike.livelikesdk.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.widget.WidgetManager
import com.livelike.livelikesdk.widget.WidgetRenderer
import com.livelike.livelikesdk.widget.asWidgetManager

class LiveLikeContentSessionImpl(
    override val programUrl: String, val currentPlayheadTime: () -> EpochTime,
    val sdkConfiguration: Provider<LiveLikeSDK.SdkConfiguration>,
    val applicationContext: Context
) : LiveLikeContentSession {

    private val llDataClient = LiveLikeDataClientImpl()
    private var program: Program? = null

    private var widgetQueue: WidgetManager? = null
    private var chatQueue: ChatQueue? = null
    private val userPreferences = applicationContext.getSharedPreferences("livelike-sdk", Context.MODE_PRIVATE)
    private val interactionSession = InteractionLogger()
    init {
        getUser()
    }

    private fun getUser() {
        val userId = userPreferences.getString("UserId", "")
        val username = userPreferences.getString("Username", "")
        if (!userId.isNullOrEmpty() && !username.isNullOrEmpty()) {
            currentUser = LiveLikeUser(userId, username)
        } else {
            sdkConfiguration.subscribe { configuration ->
                llDataClient.getLiveLikeUserData(configuration.sessionsUrl) {
                    currentUser = it
                    userPreferences.edit().putString("UserId", it.userId).apply()
                    userPreferences.edit().putString("Username", it.userName).apply()
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
    override fun getInteractionSession(): InteractionSession { return InteractionLogger() }
    private fun initializeWidgetMessaging(program: Program) {
        sdkConfiguration.subscribe {
            val widgetQueue = PubnubMessagingClient(it.pubNubKey).syncTo(currentPlayheadTime).asWidgetManager(llDataClient)
            widgetQueue.unsubscribeAll()
            widgetQueue.subscribe(listOf(program.subscribeChannel))
            widgetQueue.renderer = widgetRenderer
            this.widgetQueue = widgetQueue
        }
    }

    private fun initializeChatMessaging(program: Program) {
        sdkConfiguration.subscribe {
            val sendBirdMessagingClient = SendbirdMessagingClient(it.sendBirdAppId, applicationContext, currentUser)
            val chatQueue = sendBirdMessagingClient.syncTo(currentPlayheadTime).toChatQueue(SendbirdChatClient())
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

interface Provider<T> {
    fun subscribe(ready: (T) -> Unit)
}
