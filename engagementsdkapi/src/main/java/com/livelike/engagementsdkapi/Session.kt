package com.livelike.engagementsdkapi

import android.widget.FrameLayout
import com.google.gson.JsonObject

/**
 *  Represents a Content Session which LiveLike uses to deliver widgets and associate user with the Chat
 *  component.
 */
interface LiveLikeContentSession {
    var chatRenderer: ChatRenderer?

    /** The current livelike User **/
    val currentUser: LiveLikeUser?

    /** Where the chat data are stored **/
    val chatViewModel: ChatViewModel

    /** The analytics services **/
    val analyticService: AnalyticsService

    /** Pause the current Chat and widget sessions. This generally happens when ads are presented */
    fun pause()
    /** Resume the current Chat and widget sessions. This generally happens when ads are completed */
    fun resume()
    /** Closes the current session.*/
    fun close()
    /** Return the playheadTime for this session.*/
    fun getPlayheadTime(): EpochTime
    /** Return the content Session Id (Program Id) for this session.*/
    fun contentSessionId(): String
    /** Set the widget container. Recommended to use widgetView.SetSession(session) instead.*/
    fun setWidgetContainer(widgetView: FrameLayout)
}

interface Stream<T> {
    fun onNext(data1: T?)
    fun subscribe(key: Any, observer: (T?) -> Unit)
    fun unsubscribe(key: Any)
    fun clear()
}

class WidgetInfos(
    val type: String,
    val payload: JsonObject,
    val widgetId: String
)
