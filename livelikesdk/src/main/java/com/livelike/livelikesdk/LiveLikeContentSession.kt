package com.livelike.livelikesdk

/**
 *  Represents a playable session which LiveLike uses deliver widgets and associate user with the Chat
 *  component. SDK would keep a track of video player's current position as well. This would help the
 *  SDK to perform various synchronization operations.
 *  TODO: These can be serializable? Maybe not sure.
 */
interface LiveLikeContentSession {
    var contentSessionId : String

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
}