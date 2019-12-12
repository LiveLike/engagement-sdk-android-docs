package com.livelike.engagementsdk

import android.widget.FrameLayout
import com.google.gson.JsonObject
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import java.util.Calendar

/**
 *  Represents a Content Session which LiveLike uses to deliver widgets and associate user with the Chat
 *  component.
 */
interface LiveLikeContentSession {

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
    /** Set the user profile pic. to be shown in chatting*/
    fun setProfilePicUrl(url: String)
    /** Enter a Chat Room */
    /** Join a Chat Room, membership will be created for this room */
    fun joinChatRoom(chatRoomId: String, timestamp: Long = Calendar.getInstance().timeInMillis)
    /** Leave a Chat Room, membership will be cancelled with this room */
    fun leaveChatRoom(chatRoomId: String)
    /** Enter a Chat Room, the last entered Chat Room will be the active one */
    fun enterChatRoom(chatRoomId: String)
    /** The current active chat room, it is the last entered chat room */
    var getActiveChatRoom: () -> String
    /** Exit the specified Chat Room */
    fun exitChatRoom(chatRoomId: String)
    /** Exit all the Connected Chat Rooms */
    fun exitAllConnectedChatRooms()
    /** Returns the number of messages published on a chatroom since a given time*/
    fun getMessageCount(chatRoomId: String, startTimestamp: Long, callback: LiveLikeCallback<Long>)
    /** Register a message count listner for the specified Chat Room */
    fun setMessageListener(messageListener: MessageListener)
    /** Intercepts the widgets and hold them until show() or dismiss() is being called */
    var widgetInterceptor: WidgetInterceptor?
    /** set value of style for widget **/
    fun setWidgetViewThemeAttribute(widgetViewThemeAttributes: WidgetViewThemeAttributes)
}

/**
 * Returns the new message count whenever a unread message is being posted
 *
 */
interface MessageListener {
    fun onNewMessage(chatRoom: String, message: LiveLikeChatMessage)
}

/** A simple representation of an observable stream.
 * Subscription will requires a key to avoid multiple subscription of the same observable.
 */
interface Stream<T> {
// TODO replace all usage of Stream by Flow
    /** Post data to the stream */
    fun onNext(data1: T?)
    /** Add an observable to receive future values of the stream */
    fun subscribe(key: Any, observer: (T?) -> Unit)
    /** Stop the observable at {key} from receiving events */
    fun unsubscribe(key: Any)
    /** Remove all the observable from this stream */
    fun clear()
    /** Get the latest value of the stream */
    fun latest(): T?
}

/** A representation of a widget */
class WidgetInfos(
    /** The type of the widget */
    val type: String,
    /** The data used to define the widget */
    val payload: JsonObject,
    /** The id of the widget */
    val widgetId: String
)
