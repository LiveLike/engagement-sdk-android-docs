package com.livelike.livelikesdk
import com.livelike.livelikesdk.messaging.EpochTime
import com.livelike.livelikesdk.widget.WidgetRenderer

/**
 *  Represents a playable session which LiveLike uses deliver widgets and associate user with the Chat
 *  component. SDK would keep a track of video player's current position as well. This would help the
 *  SDK to perform various synchronization operations.
 *  TODO: These can be serializable? Maybe not sure.
 */
interface LiveLikeContentSession {
    var programUrl : String
    var renderer: WidgetRenderer?
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

interface LiveLikeDataClient {
    fun getLiveLikeProgramData(url: String, responseCallback: (program: Program) -> Unit)
}


data class Program (val programUrl: String,
                    val timelineUrl: String,
                    val clientId: String,
                    val id: String,
                    val title: String,
                    val created: Long,
                    val started: Long,
                    val widgetsEnabled: Boolean,
                    val chatEnabled: Boolean,
                    val subscribeChannel: String,
                    val chatChannel: String,
                    val streamUrl: String)
