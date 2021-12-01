package com.livelike.engagementsdk.publicapis

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.LiveLikeChatClient
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LeaderBoard
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntry
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntryPaginationResult
import com.livelike.engagementsdk.core.data.models.LeaderboardClient
import com.livelike.engagementsdk.gamification.Badges
import com.livelike.engagementsdk.gamification.IRewardsClient
import com.livelike.engagementsdk.sponsorship.Sponsor
import com.livelike.engagementsdk.widget.domain.LeaderBoardDelegate
import com.livelike.engagementsdk.widget.domain.UserProfileDelegate

interface IEngagement {

//    TODO add remaining public SDK functions in this interface
    /** The analytics services **/
    val analyticService: Stream<AnalyticsService>

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

    /* Set user profile delegate to intercept any user related updates like rewards */
    var userProfileDelegate: UserProfileDelegate?

    var leaderBoardDelegate: LeaderBoardDelegate?

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#chatRoomListener} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#chatRoomListener} instead like this")
    var chatRoomDelegate: ChatRoomDelegate?

    /** Override the default auto-generated chat nickname **/
    fun updateChatNickname(nickname: String)

    /** Override the default auto-generated chat userpic **/
    fun updateChatUserPic(url: String?)

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#createChatRoom} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#createChatRoom} instead like this")
    fun createChatRoom(
        title: String? = null,
        visibility: Visibility? = null,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    )

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#updateChatRoom} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#updateChatRoom} instead like this")
    fun updateChatRoom(
        chatRoomId: String,
        title: String? = null,
        visibility: Visibility? = null,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    )

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getChatRoom} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getChatRoom} instead like this")
    fun getChatRoom(id: String, liveLikeCallback: LiveLikeCallback<ChatRoomInfo>)

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#addCurrentUserToChatRoom} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#addCurrentUserToChatRoom} instead like this")
    fun addCurrentUserToChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomMembership>
    )
    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#addUserToChatRoom} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#addUserToChatRoom} instead like this")
    fun addUserToChatRoom(
        chatRoomId: String,
        userId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomMembership>
    )
    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getCurrentUserChatRoomList} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getCurrentUserChatRoomList} instead like this")
    fun getCurrentUserChatRoomList(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInfo>>
    )
    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getMembersOfChatRoom} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getMembersOfChatRoom} instead like this")
    fun getMembersOfChatRoom(
        chatRoomId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<LiveLikeUser>>
    )
    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#deleteCurrentUserFromChatRoom} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#deleteCurrentUserFromChatRoom} instead like this")
    fun deleteCurrentUserFromChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
    )

    fun getLeaderBoardsForProgram(
        programId: String,
        liveLikeCallback: LiveLikeCallback<List<LeaderBoard>>
    )

    fun getLeaderBoardDetails(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoard>
    )

    fun getEntriesForLeaderBoard(
        leaderBoardId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntryPaginationResult>
    )

    fun getLeaderBoardEntryForProfile(
        leaderBoardId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    )

    fun getLeaderBoardEntryForCurrentUserProfile(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    )

    fun getLeaderboardClients(
        leaderBoardId: List<String>,
        liveLikeCallback: LiveLikeCallback<LeaderboardClient>
    )

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getProfileMutedStatus} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getProfileMutedStatus} instead like this")
    fun getChatUserMutedStatus(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatUserMuteStatus>
    )

    fun getCurrentUserDetails(liveLikeCallback: LiveLikeCallback<LiveLikeUserApi>)

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#sendChatRoomInviteToUser} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#sendChatRoomInviteToUser} instead like this")
    fun sendChatRoomInviteToUser(
        chatRoomId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomInvitation>
    )
    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#updateChatRoomInviteStatus} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#updateChatRoomInviteStatus} instead like this")
    fun updateChatRoomInviteStatus(
        chatRoomInvitation: ChatRoomInvitation,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<ChatRoomInvitation>
    )

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getInvitationsReceivedByCurrentProfileWithInvitationStatus} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getInvitationsReceivedByCurrentProfileWithInvitationStatus} instead like this")
    fun getInvitationsForCurrentProfileWithInvitationStatus(
        liveLikePagination: LiveLikePagination,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInvitation>>
    )

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getInvitationsSentByCurrentProfileWithInvitationStatus} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getInvitationsSentByCurrentProfileWithInvitationStatus} instead like this")
    fun getInvitationsByCurrentProfileWithInvitationStatus(
        liveLikePagination: LiveLikePagination,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInvitation>>
    )

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#blockProfile} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#blockProfile} instead like this")
    fun blockProfile(
        profileId: String,
        liveLikeCallback: LiveLikeCallback<BlockedData>
    )

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#unBlockProfile} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#unBlockProfile} instead like this")
    fun unBlockProfile(
        blockId: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
    )

    /**
     *@deprecated  reason this method is deprecated <br/>
     *              {will be removed in next version} <br/>
     *              use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getBlockedProfileList} instead like this:
     */
    @Deprecated("use {@link com.livelike.engagementsdk.chat.LiveLikeChatClient#getBlockedProfileList} instead like this")
    fun getBlockedProfileList(
        liveLikePagination: LiveLikePagination,
        blockedProfileId: String?,
        liveLikeCallback: LiveLikeCallback<List<BlockedData>>
    )

    /**
     * Returns the sponsor client
     */
    fun sponsor(): Sponsor

    /**
     * Returns the Badges client
     */
    fun badges(): Badges

    /**
     * Returns the Rewards client
     */
    fun rewards(): IRewardsClient

    fun close()

    fun chat(): LiveLikeChatClient
}
