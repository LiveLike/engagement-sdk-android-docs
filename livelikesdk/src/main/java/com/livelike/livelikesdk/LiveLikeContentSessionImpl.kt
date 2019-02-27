package com.livelike.livelikesdk

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
    override val programUrl: String, val currentPlayheadTime: () -> EpochTime
    , val sdkConfiguration: LiveLikeSDK.SdkConfiguration
) : LiveLikeContentSession {

    private val llDataClient = LiveLikeDataClientImpl()
    private var program: Program? = null

    //Implement base Queue type? with toggleable (pause/resume) and biderectional hooks?
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
            if(renderer != null && program != null)
                initializeChatMessaging()
        }

    init {
        llDataClient.getLiveLikeProgramData(programUrl) {
            program = it
            initializeWidgetMessaging()
            initializeChatMessaging()
        }
    }

    override fun getPlayheadTime(): EpochTime {
        return currentPlayheadTime()
    }

    override fun contentSessionId() = program?.clientId ?: ""

    private fun initializeWidgetMessaging() {
        val pubNubMessagingClient = PubnubMessagingClient(sdkConfiguration.pubNubKey)
        val widgetQueue = pubNubMessagingClient.syncTo(currentPlayheadTime).toWidgetQueue()
        widgetQueue.subscribe(listOf(program!!.subscribeChannel))
        widgetQueue.renderer = widgetRenderer
        this.widgetQueue = widgetQueue
    }

    private fun initializeChatMessaging() {
        if(chatRenderer != null) {
            val sendBirdMessagingClient = SendbirdMessagingClient(sdkConfiguration.sendBirdAppId, chatRenderer!!.chatContext)
            val chatQueue = sendBirdMessagingClient.syncTo(currentPlayheadTime).toChatQueue(SendbirdChatClient())
            chatQueue.subscribe(listOf(program!!.chatChannel))
            chatQueue.renderer = chatRenderer
            this.chatQueue = chatQueue
        } else {

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
