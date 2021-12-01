package com.livelike.engagementsdk.chat

import com.example.example.PinMessageInfo
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.chat.data.remote.PinMessageOrder
import com.livelike.engagementsdk.publicapis.*
import com.livelike.engagementsdk.publicapis.*

interface LiveLikeChatClient {

    var chatRoomDelegate: ChatRoomDelegate?

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
        liveLikeCallback: LiveLikeCallback<BlockedInfo>
    )

    fun unBlockProfile(
        blockId: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
    )

    fun getBlockedProfileList(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<BlockedInfo>>
    )

    fun getProfileMutedStatus(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatUserMuteStatus>
    )

    fun getProfileBlockInfo(profileId: String, liveLikeCallback: LiveLikeCallback<BlockedInfo>)


    fun pinMessage(
        messageId: String,
        chatRoomId: String,
        chatMessagePayload: LiveLikeChatMessage,
        liveLikeCallback: LiveLikeCallback<PinMessageInfo>
    )

    fun unPinMessage(
        pinMessageInfoId: String,
        liveLiveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
    )

    fun getPinMessageInfoList(
        chatRoomId: String,
        order: PinMessageOrder,
        pagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<PinMessageInfo>>
    )
}