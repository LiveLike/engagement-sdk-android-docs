package com.livelike.engagementsdk.publicapis

import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembershipPagination

interface IEngagement {

//    TODO add remaining public SDK functions in this interface

    /**
     *  Returns access token associated with user to be used for future sdk initialization.
     *  This access token acts as a unique identifier for a user profile in LiveLike system.
     *  Null value means sdk initialization process not completed.
     */
    val userAccessToken: String?

    /**
     * Returns public user stream.
     */
    val userStream: Stream<LiveLikeUserApi>

    /** Override the default auto-generated chat nickname **/
    fun updateChatNickname(nickname: String)

    /** Override the default auto-generated chat userpic **/
    fun updateChatUserPic(url: String?)

    fun createChatRoom(title: String? = null, liveLikeCallback: LiveLikeCallback<ChatRoomInfo>)

    fun getChatRoom(id: String, liveLikeCallback: LiveLikeCallback<ChatRoomInfo>)

    fun addCurrentUserToChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomMembership>
    )

    fun getCurrentUserChatRoomList(
        chatRoomMembershipPagination: ChatRoomMembershipPagination,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInfo>>
    )
<<<<<<< Updated upstream
=======

>>>>>>> Stashed changes
    fun getMembersOfChatRoom(
        chatRoomId: String,
        chatRoomMembershipPagination: ChatRoomMembershipPagination,
        liveLikeCallback: LiveLikeCallback<List<LiveLikeUser>>
    )

    fun deleteCurrentUserFromChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<Boolean>
    )
}
