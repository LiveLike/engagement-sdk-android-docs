package com.livelike.livelikesdk

import com.google.gson.JsonObject
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.proxies.syncTo
import com.livelike.livelikesdk.messaging.pubnub.PubnubMessagingClient
import com.livelike.livelikesdk.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.widget.WidgetQueue
import com.livelike.livelikesdk.widget.WidgetRenderer
import com.livelike.livelikesdk.widget.toWidgetQueue


class LiveLikeContentSessionImpl(override var programUrl: String, val currentPlayheadTime: () -> EpochTime
) : LiveLikeContentSession {

    private val llDataClient = LiveLikeDataClientImpl()
    private var program: Program? = null
    private var pubNubMessagingClient: MessagingClient? = null
    private var sendBirdChatClient : MessagingClient? = null
    private var widgetQueue: WidgetQueue? = null
    override var renderer: WidgetRenderer? = null
        set(value) {
            field = value
            widgetQueue?.renderer = renderer
        }

    override fun getPlayheadTime(): EpochTime {
        return currentPlayheadTime()
    }

    override fun contentSessionId(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    init {
        llDataClient.getLiveLikeProgramData(programUrl) {
            program = Program(
                it.extractStringOrEmpty("url"),
                it.extractStringOrEmpty("timeline_url"),
                it.extractStringOrEmpty("content_id"),
                it.extractStringOrEmpty("id"),
                it.extractStringOrEmpty("title"),
                it.extractLong("created_at"),
                it.extractLong("started_at"),
                it["widgets_enabled"].asBoolean,
                it["chat_enabled"].asBoolean,
                it.extractStringOrEmpty("subscribe_channel"),
                it.extractStringOrEmpty("sendbird_channel"),
                it.extractStringOrEmpty("stream_url"))
            //TODO check against empty program
            initializeWidgetMessaging()

        }
    }

    private fun initializeWidgetMessaging() {
        pubNubMessagingClient = PubnubMessagingClient(program!!.clientId)
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

fun JsonObject.extractStringOrEmpty(propertyName: String): String {
    return if (this.has(propertyName) && !this[propertyName].isJsonNull) this[propertyName].asString else ""
}

fun JsonObject.extractLong(propertyName: String, default: Long = 0): Long {
    var returnVal = default
    try {
        returnVal = if (this.has(propertyName) && !this[propertyName].isJsonNull) this[propertyName].asLong else default
    } catch (e: NumberFormatException) {}
    return returnVal
}
