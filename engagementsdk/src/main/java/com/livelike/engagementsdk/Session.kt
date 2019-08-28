package com.livelike.engagementsdk

import android.widget.FrameLayout
import com.google.gson.JsonObject
import com.livelike.engagementsdk.chat.ChatViewModel
import com.livelike.engagementsdk.services.messaging.proxies.WidgetInterceptor

/**
 *  Represents a Content Session which LiveLike uses to deliver widgets and associate user with the Chat
 *  component.
 */
interface LiveLikeContentSession {
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

    var widgetInterceptor: WidgetInterceptor?
}

interface Stream<T> {
    fun onNext(data1: T?)
    fun subscribe(key: Any, observer: (T?) -> Unit)
    fun unsubscribe(key: Any)
    fun clear()
    fun latest(): T?
}

class WidgetInfos(
    val type: String,
    val payload: JsonObject,
    val widgetId: String
)
