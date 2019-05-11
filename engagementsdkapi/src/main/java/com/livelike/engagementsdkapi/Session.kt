package com.livelike.engagementsdkapi

import android.content.Context
import android.view.View

/**
 *  Represents a Content Session which LiveLike uses to deliver widgets and associate user with the Chat
 *  component.
 */
interface LiveLikeContentSession {
    val programUrl: String
    var widgetRenderer: WidgetRenderer?
    var chatRenderer: ChatRenderer?
    val currentUser: LiveLikeUser?
    val chatState: ChatState
    var widgetState: WidgetTransientState
    val widgetStream: WidgetStream
    val applicationContext: Context

    /** Pause the current Chat and widget sessions. This generally happens when ads are presented */
    fun pause()
    /** Resume the current Chat and widget sessions. This generally happens when ads are completed */
    fun resume()
    /** Clear the user's chat history. */
    fun clearChatHistory()
    /** Clear the feedback queue. */
    fun clearFeedbackQueue()
    /** Closes the current session.*/
    fun close()

    /** Return the playheadTime for this session.*/
    fun getPlayheadTime(): EpochTime

    fun contentSessionId(): String
}

interface WidgetStream {
    fun onNext(view: View?)
    fun subscribe(key: Any, observer: (View?) -> Unit)
    fun unsubscribe(key: Any)
    fun clear()
}