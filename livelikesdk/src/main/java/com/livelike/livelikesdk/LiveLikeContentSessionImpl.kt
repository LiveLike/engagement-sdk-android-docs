package com.livelike.livelikesdk

import android.content.Context
import com.livelike.livelikesdk.chat.ChatQueue
import com.livelike.livelikesdk.chat.ChatRenderer
import com.livelike.livelikesdk.chat.toChatQueue
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.proxies.syncTo
import com.livelike.livelikesdk.messaging.pubnub.PubnubMessagingClient
import com.livelike.livelikesdk.messaging.sendbird.SendbirdChatClient
import com.livelike.livelikesdk.messaging.sendbird.SendbirdMessagingClient
import com.livelike.livelikesdk.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.widget.WidgetQueue
import com.livelike.livelikesdk.widget.WidgetRenderer
import com.livelike.livelikesdk.widget.toWidgetQueue


class LiveLikeContentSessionImpl(
    override val programUrl: String, val currentPlayheadTime: () -> EpochTime,
    val sdkConfiguration: Provider<LiveLikeSDK.SdkConfiguration>,
    val applicationContext: Context
) : LiveLikeContentSession {

    private val llDataClient = LiveLikeDataClientImpl()
    private var program: Program? = null

    private var widgetQueue: WidgetQueue? = null
    private var chatQueue: ChatQueue? = null

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
            val pubNubMessagingClient = PubnubMessagingClient(it.pubNubKey)
            val widgetQueue = pubNubMessagingClient.syncTo(currentPlayheadTime).toWidgetQueue()
            widgetQueue.unsubscribeAll()
            widgetQueue.subscribe(listOf(program.subscribeChannel))
            widgetQueue.renderer = widgetRenderer
            this.widgetQueue = widgetQueue
        }
    }

    private fun initializeChatMessaging(program: Program) {
        sdkConfiguration.subscribe {
            val sendBirdMessagingClient = SendbirdMessagingClient(it.sendBirdAppId, applicationContext)
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
