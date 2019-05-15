package com.livelike.engagementsdkapi

import com.google.gson.JsonObject

/**
 *  Represents a Content Session which LiveLike uses to deliver widgets and associate user with the Chat
 *  component.
 */
interface LiveLikeContentSession {
    var programUrl: String
    var currentPlayheadTime: () -> EpochTime // need to be replace by a proper time getter Java compatible
    var widgetRenderer: WidgetRenderer?
    var chatRenderer: ChatRenderer?
    val currentUser: LiveLikeUser?
    val chatState: ChatState
    var widgetState: WidgetTransientState
    val widgetStream: Stream<String?, JsonObject?>

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

interface Stream<T, T2> {
    fun onNext(data1: T?, data2: T2?)
    fun subscribe(key: Any, observer: (T?, T2?) -> Unit)
    fun unsubscribe(key: Any)
    fun clear()
}