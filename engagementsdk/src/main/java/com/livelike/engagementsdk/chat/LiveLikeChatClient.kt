package com.livelike.engagementsdk.chat

import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.BlockedData
import com.livelike.engagementsdk.publicapis.ChatRoomInvitation
import com.livelike.engagementsdk.publicapis.ChatRoomInvitationStatus
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeEmptyResponse

interface LiveLikeChatClient {
    fun createChatRoom(
        title: String? = null,
        visibility: Visibility? = null,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    )

    fun updateChatRoom(
        chatRoomId: String,
        title: String? = null,
        visibility: Visibility? = null,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    )

    fun getChatRoom(id: String, liveLikeCallback: LiveLikeCallback<ChatRoomInfo>)

    fun addCurrentUserToChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomMembership>
    )

    fun addUserToChatRoom(
        chatRoomId: String,
        userId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomMembership>
    )

    fun getCurrentUserChatRoomList(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInfo>>
    )

    fun getMembersOfChatRoom(
        chatRoomId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<LiveLikeUser>>
    )

    fun deleteCurrentUserFromChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
    )

    fun sendChatRoomInviteToUser(
        chatRoomId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomInvitation>
    )

    fun updateChatRoomInviteStatus(
        chatRoomInvitation: ChatRoomInvitation,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<ChatRoomInvitation>
    )

    fun getInvitationsReceivedByCurrentProfileWithInvitationStatus(
        liveLikePagination: LiveLikePagination,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInvitation>>
    )

    fun getInvitationsSentByCurrentProfileWithInvitationStatus(
        liveLikePagination: LiveLikePagination,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInvitation>>
    )

    fun blockProfile(
        profileId: String,
        liveLikeCallback: LiveLikeCallback<BlockedData>
    )

    fun unBlockProfile(
        blockId: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
    )

    fun getBlockedProfileList(
        liveLikePagination: LiveLikePagination,
        blockedProfileId: String?,
        liveLikeCallback: LiveLikeCallback<List<BlockedData>>
    )
}