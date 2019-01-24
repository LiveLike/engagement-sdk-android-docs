package com.livelike.livelikesdk

class LiveLikeContentSessionImpl(override var contentSessionId: String,
                                 currentPlayheadTime: (Long) -> Unit) : LiveLikeContentSession {

    private val playheadTimeSource = currentPlayheadTime
    private var contentId : String = contentSessionId

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