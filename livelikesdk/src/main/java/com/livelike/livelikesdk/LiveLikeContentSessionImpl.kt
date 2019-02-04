package com.livelike.livelikesdk

import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.MessagingEventListener
import com.livelike.livelikesdk.messaging.pubnub.PubnubMessagingClient

class LiveLikeContentSessionImpl(override var contentSessionId: String,
                                 currentPlayheadTime: (Long) -> Unit) : LiveLikeContentSession {

    private val playheadTimeSource = currentPlayheadTime
    private var contentId : String = contentSessionId
    private val widgetMessagingClient : MessagingClient = PubnubMessagingClient(contentId)

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

    override fun setWidgetSourceListener(listener: MessagingEventListener) {
        widgetMessagingClient.addMessagingEventListener(listener)
    }
}
