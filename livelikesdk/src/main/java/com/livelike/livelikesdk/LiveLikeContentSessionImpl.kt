package com.livelike.livelikesdk

import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.proxies.syncTo
import com.livelike.livelikesdk.messaging.pubnub.PubnubMessagingClient
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
    private var pubNubMessagingClient: MessagingClient? = null
    private var widgetQueue: WidgetQueue? = null
    override var renderer: WidgetRenderer? = null
        set(value) {
            field = value
            widgetQueue?.renderer = renderer
        }

    override fun getPlayheadTime(): EpochTime {
        return currentPlayheadTime()
    }

    override fun contentSessionId() = program?.clientId ?: ""


    init {
        llDataClient.getLiveLikeProgramData(programUrl) {
            program = it
            initializeWidgetMessaging()
        }
    }

    private fun initializeWidgetMessaging() {
        pubNubMessagingClient = PubnubMessagingClient(sdkConfiguration.pubNubKey)
        widgetQueue = pubNubMessagingClient!!.syncTo(currentPlayheadTime).toWidgetQueue()
        widgetQueue!!.subscribe(listOf(program!!.subscribeChannel))
        widgetQueue!!.renderer = renderer
    }

    private fun initializeChatMessaging() {

    }

    override fun pause() {
        widgetQueue?.toggleEmission(true)
    }

    override fun resume() {
        widgetQueue?.toggleEmission(false)
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
