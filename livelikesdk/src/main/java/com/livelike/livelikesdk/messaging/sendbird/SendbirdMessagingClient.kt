package com.livelike.livelikesdk.messaging.sendbird

import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.MessagingEventListener

class SendbirdMessagingClient (val contentId: String) : MessagingClient {

    private var listener : MessagingEventListener? = null

    init {
        val subscribeKey = fetchSubKey(contentId)
        val username = fetchUsername()
    }

    private fun fetchUsername() : String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun fetchSubKey(contentId: String): String {
        //TODO: Get sendbird sub key from content id when backend Application endpoint is integrated.
        return "sub-c-016db434-d156-11e8-b5de-7a9ddb77e130"
    }

    override fun subscribe(channels: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unsubscribe(channels: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unsubscribeAll() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addMessagingEventListener(listener: MessagingEventListener) {
        this.listener = listener
    }

}
