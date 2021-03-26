package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import java.util.Calendar

/**
 * Created by Shivansh Mittal on 2020-04-08.
 */
interface LiveLikeChatSession {

    /** Return the playheadTime for this session.*/
    fun getPlayheadTime(): EpochTime

    /** Pause the current Chat and widget sessions. This generally happens when ads are presented */
    fun pause()

    /** Resume the current Chat and widget sessions. This generally happens when ads are completed */
    fun resume()

    /** Closes the current session.*/
    fun close()

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
    fun getMessageCount(chatRoomId: String, startTimestamp: Long, callback: LiveLikeCallback<Byte>)

    /** Register a message count listner for the specified Chat Room */
    fun setMessageListener(messageListener: MessageListener)

    /** Set the value of visibility of chat avatar **/
    var shouldDisplayAvatar: Boolean

    /** Avatar Image Url  **/
    var avatarUrl: String?

    /**
     * send Chat Message to the current ChatRoom
     *
     * @message : text message
     * @imageUrl: image message
     * @imageWidth: image width default is 100
     * @imageHeight: image height default is 100
     * @liveLikeCallback : callback to provide the message object, this callback is not meant the message is sent
     * **/
    fun sendChatMessage(
        message: String?,
        imageUrl: String? = null,
        imageWidth: Int? = 100,
        imageHeight: Int? = 100,
        liveLikeCallback: LiveLikeCallback<LiveLikeChatMessage>
    )
}
