package com.livelike.livelikesdk

import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.proxies.syncTo
import com.livelike.livelikesdk.messaging.pubnub.PubnubMessagingClient
import com.livelike.livelikesdk.widget.WidgetQueue
import com.livelike.livelikesdk.widget.WidgetRenderer
import com.livelike.livelikesdk.widget.toWidgetQueue

class LiveLikeContentSessionImpl(override var contentSessionId: String,
                                 currentPlayheadTime: (Long) -> EpochTime) : LiveLikeContentSession {
    private val playheadTimeSource = currentPlayheadTime
    private var contentId : String = contentSessionId
    private val pubNubMessagingClient : MessagingClient = PubnubMessagingClient(contentId)
    private var widgetQueue: WidgetQueue? = null

    override var renderer : WidgetRenderer? = null
    set(value) {
        field = value
        widgetQueue = pubNubMessagingClient.syncTo(playheadTimeSource.invoke(0)).toWidgetQueue()
        widgetQueue?.renderer = renderer
        widgetQueue?.subscribe(listOf("program_642f635d_44a6_4e2a_b638_504021f62f6a"))
    }
    
    override fun pause() {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resume() {
       // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
