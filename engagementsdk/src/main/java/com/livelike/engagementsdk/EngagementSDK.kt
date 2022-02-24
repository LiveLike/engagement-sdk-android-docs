package com.livelike.engagementsdk

import android.content.Context
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import com.jakewharton.threetenabp.AndroidThreeTen
import com.livelike.engagementsdk.chat.*
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.livelike.engagementsdk.core.data.models.*
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.*
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getSharedAccessToken
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.setSharedAccessToken
import com.livelike.engagementsdk.gamification.*
import com.livelike.engagementsdk.publicapis.*
import com.livelike.engagementsdk.sponsorship.ISponsor
import com.livelike.engagementsdk.sponsorship.Sponsor
import com.livelike.engagementsdk.widget.data.respository.LocalPredictionWidgetVoteRepository
import com.livelike.engagementsdk.widget.data.respository.PredictionWidgetVoteRepository
import com.livelike.engagementsdk.widget.domain.LeaderBoardDelegate
import com.livelike.engagementsdk.widget.domain.UserProfileDelegate
import com.livelike.engagementsdk.widget.services.network.WidgetDataClientImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.io.IOException

/**
 * Use this class to initialize the EngagementSDK. This is the entry point for SDK usage. This creates an instance of EngagementSDK.
 *
 * @param clientId Client's id
 * @param applicationContext The application context
 */
class EngagementSDK(
    private val clientId: String,
    private val applicationContext: Context,
    private val errorDelegate: ErrorDelegate? = null,
    private val originURL: String? = null,
    private var accessTokenDelegate: AccessTokenDelegate? = null
) : IEngagement {

    private var internalChatClient: InternalLiveLikeChatClient
    internal var configurationStream: Stream<SdkConfiguration> =
        SubscriptionManager(true)
    private val dataClient =
        EngagementDataClientImpl()
    private val widgetDataClient = WidgetDataClientImpl()

    internal val userRepository =
        UserRepository(clientId)

    override var userProfileDelegate: UserProfileDelegate? = null
        set(value) {
            field = value
            userRepository.userProfileDelegate = value
        }

    override var leaderBoardDelegate: LeaderBoardDelegate? = null
        set(value) {
            field = value
            userRepository.leaderBoardDelegate = value
        }
    override var chatRoomDelegate: ChatRoomDelegate? = null
        set(value) {
            field = value
            chat().chatRoomDelegate = value
        }

    override var analyticService: Stream<AnalyticsService> =
        SubscriptionManager()

    private val job = SupervisorJob()

    // by default sdk calls will run on Default pool and further data layer calls will run o
    internal val sdkScope = CoroutineScope(Dispatchers.Default + job)

    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    // sdk config-user flow that can be collected by collect which is suspendably instead of using rx style combine on 2 seperate async results
    // TODO add util fun to convert streams to flow
    internal val configurationUserPairFlow = flow {
        while (configurationStream.latest() == null || userRepository.currentUserStream.latest() == null) {
            delay(1000)
        }
        emit(Pair(userRepository.currentUserStream.latest()!!, configurationStream.latest()!!))
    }

    /**
     * SDK Initialization logic.
     */
    init {
        AndroidThreeTen.init(applicationContext) // Initialize DateTime lib
        initLiveLikeSharedPrefs(
            applicationContext
        )
        if (accessTokenDelegate == null) {
            accessTokenDelegate = object : AccessTokenDelegate {
                override fun getAccessToken(): String? = getSharedAccessToken()

                override fun storeAccessToken(accessToken: String?) {
                    accessToken?.let { setSharedAccessToken(accessToken) }
                }
            }
        }
        internalChatClient =
            InternalLiveLikeChatClient(configurationUserPairFlow, uiScope, sdkScope, dataClient)
        userRepository.currentUserStream.subscribe(this.javaClass.simpleName) { user ->
            user?.accessToken?.let { token ->
                userRepository.currentUserStream.unsubscribe(this.javaClass.simpleName)
                accessTokenDelegate!!.storeAccessToken(token)
            }
            (chat() as InternalLiveLikeChatClient).setUpPubNubClientForChatRoom()
        }
        val url = originURL?.plus("/api/v1/applications/$clientId")
            ?: BuildConfig.CONFIG_URL.plus("applications/$clientId")
        dataClient.getEngagementSdkConfig(url) {
            if (it is Result.Success) {
                configurationStream.onNext(it.data)
                it.data.mixpanelToken?.let { token ->
                    analyticService.onNext(
                        MixpanelAnalytics(
                            applicationContext,
                            token,
                            it.data.clientId
                        )
                    )
                }
                userRepository.initUser(accessTokenDelegate!!.getAccessToken(), it.data.profileUrl)
            } else {
                errorDelegate?.onError(
                    (it as Result.Error).exception.message
                        ?: "Some Error occurred, used sdk logger for more details"
                )
            }
        }
    }

    override val userStream: Stream<LiveLikeUserApi>
        get() = userRepository.currentUserStream.map {
            LiveLikeUserApi(it.nickname, it.accessToken, it.id, it.custom_data)
        }
    override val userAccessToken: String?
        get() = userRepository.userAccessToken

    override fun updateChatNickname(nickname: String) {
        sdkScope.launch {
            userRepository.updateChatNickname(nickname)
        }
    }

    override fun updateChatUserPic(url: String?) {
        sdkScope.launch {
            userRepository.setProfilePicUrl(url)
        }
    }

    override fun createChatRoom(
        title: String?,
        visibility: Visibility?,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    ) {
        (chat() as InternalLiveLikeChatClient).createUpdateChatRoom(
            null,
            visibility,
            title,
            false,
            liveLikeCallback
        )
    }

    override fun updateChatRoom(
        chatRoomId: String,
        title: String?,
        visibility: Visibility?,
        liveLikeCallback: LiveLikeCallback<ChatRoomInfo>
    ) {
        (chat() as InternalLiveLikeChatClient).createUpdateChatRoom(
            chatRoomId,
            visibility,
            title,
            false,
            liveLikeCallback
        )
    }

    override fun getChatRoom(id: String, liveLikeCallback: LiveLikeCallback<ChatRoomInfo>) {
        chat().getChatRoom(id, liveLikeCallback)
    }

    override fun addCurrentUserToChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomMembership>
    ) {
        addUserToChatRoom(chatRoomId, "", liveLikeCallback)
    }

    override fun addUserToChatRoom(
        chatRoomId: String,
        userId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomMembership>
    ) {
        chat().addUserToChatRoom(chatRoomId, userId, liveLikeCallback)
    }

    override fun getCurrentUserChatRoomList(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInfo>>
    ) {
        chat().getCurrentUserChatRoomList(liveLikePagination, liveLikeCallback)
    }

    override fun getMembersOfChatRoom(
        chatRoomId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<LiveLikeUser>>
    ) {
        chat().getMembersOfChatRoom(chatRoomId, liveLikePagination, liveLikeCallback)
    }

    override fun deleteCurrentUserFromChatRoom(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
    ) {
        chat().deleteCurrentUserFromChatRoom(chatRoomId, liveLikeCallback)
    }

    override fun getLeaderBoardsForProgram(
        programId: String,
        liveLikeCallback: LiveLikeCallback<List<LeaderBoard>>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getLeaderBoardsForProgram(
            programId,
            liveLikeCallback
        )
    }

    override fun getLeaderBoardDetails(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoard>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getLeaderBoardDetails(
            leaderBoardId,
            liveLikeCallback
        )
    }

    override fun getLeaderboardClients(
        leaderBoardId: List<String>,
        liveLikeCallback: LiveLikeCallback<LeaderboardClient>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getLeaderboardClients(
            leaderBoardId,
            liveLikeCallback
        )
    }

    override fun getChatUserMutedStatus(
        chatRoomId: String,
        liveLikeCallback: LiveLikeCallback<ChatUserMuteStatus>
    ) {
        chat().getProfileMutedStatus(chatRoomId, liveLikeCallback)
    }

    override fun getCurrentUserDetails(liveLikeCallback: LiveLikeCallback<LiveLikeUserApi>) {
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                dataClient.getUserData(pair.second.profileUrl, pair.first.accessToken) {
                    if (it == null) {
                        liveLikeCallback.onResponse(
                            null,
                            "Network error or invalid access token"
                        )
                    } else {
                        liveLikeCallback.onResponse(
                            LiveLikeUserApi(
                                it.nickname,
                                it.accessToken,
                                it.id,
                                it.custom_data
                            ),
                            null
                        )
                    }
                }
            }
        }
    }

    override fun sendChatRoomInviteToUser(
        chatRoomId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<ChatRoomInvitation>
    ) {
        chat().sendChatRoomInviteToUser(chatRoomId, profileId, liveLikeCallback)
    }

    override fun updateChatRoomInviteStatus(
        chatRoomInvitation: ChatRoomInvitation,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<ChatRoomInvitation>
    ) {
        chat().updateChatRoomInviteStatus(
            chatRoomInvitation,
            invitationStatus,
            liveLikeCallback
        )
    }

    override fun getInvitationsForCurrentProfileWithInvitationStatus(
        liveLikePagination: LiveLikePagination,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInvitation>>
    ) {
        chat().getInvitationsReceivedByCurrentProfileWithInvitationStatus(
            liveLikePagination,
            invitationStatus,
            liveLikeCallback
        )
    }

    override fun getInvitationsByCurrentProfileWithInvitationStatus(
        liveLikePagination: LiveLikePagination,
        invitationStatus: ChatRoomInvitationStatus,
        liveLikeCallback: LiveLikeCallback<List<ChatRoomInvitation>>
    ) {
        chat().getInvitationsSentByCurrentProfileWithInvitationStatus(
            liveLikePagination,
            invitationStatus,
            liveLikeCallback
        )
    }

    override fun blockProfile(
        profileId: String,
        liveLikeCallback: LiveLikeCallback<BlockedInfo>
    ) {
        chat().blockProfile(profileId, liveLikeCallback)
    }

    override fun unBlockProfile(
        blockId: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeEmptyResponse>
    ) {
        chat().unBlockProfile(blockId, liveLikeCallback)
    }

    override fun getBlockedProfileList(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<List<BlockedInfo>>
    ) {
        chat().getBlockedProfileList(liveLikePagination, liveLikeCallback)
    }

    override fun sponsor(): ISponsor {
        return Sponsor(
            configurationUserPairFlow,
            dataClient,
            sdkScope,
            userRepository,
            chat(),
            uiScope
        )
    }

    override fun badges(): Badges {
        return Badges(configurationStream, dataClient, sdkScope)
    }

    override fun rewards(): IRewardsClient {
        return Rewards(configurationUserPairFlow, dataClient, sdkScope)
    }

    /**
     * Closing all the services , stream and clear the variable
     * TODO: all stream close,instance clear
     */
    override fun close() {
        (chat() as InternalLiveLikeChatClient).pubnubClient?.destroy()
        analyticService.latest()?.destroy()
        analyticService.clear()
    }

    override fun chat(): LiveLikeChatClient = internalChatClient

    private val internalLeaderBoardClient =
        InternalLiveLikeLeaderBoardClient(configurationStream, userRepository, uiScope, dataClient)

    override fun leaderboard(): LiveLikeLeaderBoardClient = internalLeaderBoardClient


    override fun getEntriesForLeaderBoard(
        leaderBoardId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntryPaginationResult>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getEntriesForLeaderBoard(
            leaderBoardId,
            liveLikePagination,
            liveLikeCallback
        )
    }

    override fun getLeaderBoardEntryForProfile(
        leaderBoardId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getLeaderBoardEntryForProfile(
            leaderBoardId,
            profileId,
            liveLikeCallback
        )
    }


    override fun getLeaderBoardEntryForCurrentUserProfile(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    ) {
        (leaderboard() as InternalLiveLikeLeaderBoardClient).getLeaderBoardEntryForCurrentUserProfile(
            leaderBoardId,
            liveLikeCallback
        )

    }

    fun fetchWidgetDetails(
        widgetId: String,
        widgetKind: String,
        liveLikeCallback: LiveLikeCallback<LiveLikeWidget>
    ) {
        uiScope.launch {
            try {
                val jsonObject = widgetDataClient.getWidgetDataFromIdAndKind(widgetId, widgetKind)
                val widget = gson.fromJson(jsonObject, LiveLikeWidget::class.java)
                liveLikeCallback.onResponse(
                    widget,
                    null
                )
            } catch (e: JsonParseException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            } catch (e: IOException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            }
        }
    }

    /**
     *  Creates a content session without sync.
     *  @param programId Backend generated unique identifier for current program
     */
    fun createContentSession(
        programId: String,
        errorDelegate: ErrorDelegate? = null
    ): LiveLikeContentSession {
        return ContentSession(
            configurationStream,
            userRepository,
            applicationContext,
            programId,
            analyticService,
            errorDelegate,
            chat()
        ) { EpochTime(0) }
    }

    /**
     * Use to retrieve the current timecode from the videoplayer to enable Spoiler-Free Sync.
     *
     */
    interface TimecodeGetter {
        fun getTimecode(): EpochTime
    }

    /**
     *  Creates a content session with sync.
     *  @param programId Backend generated identifier for current program
     *  @param timecodeGetter returns the video timecode
     */
    fun createContentSession(
        programId: String,
        timecodeGetter: TimecodeGetter,
        errorDelegate: ErrorDelegate? = null
    ): LiveLikeContentSession {
        return ContentSession(
            configurationStream,
            userRepository,
            applicationContext,
            programId,
            analyticService,
            errorDelegate,
            chat()
        ) { timecodeGetter.getTimecode() }.apply {
            this.engagementSDK = this@EngagementSDK
        }
    }

    /**
     *  Creates a chat session.
     *  @param programId Backend generated identifier for current program
     *  @param timecodeGetter returns the video timecode
     */
    fun createChatSession(
        timecodeGetter: TimecodeGetter,
        errorDelegate: ErrorDelegate? = null
    ): LiveLikeChatSession {
        return ChatSession(
            configurationStream,
            userRepository,
            applicationContext,
            false,
            analyticService,
            errorDelegate,
            chat(),
        ) { timecodeGetter.getTimecode() }
    }

    internal data class SdkConfiguration(
        val url: String,
        val name: String?,
        @SerializedName("client_id")
        val clientId: String,
        @SerializedName("media_url")
        val mediaUrl: String,
        @SerializedName("pubnub_subscribe_key")
        val pubNubKey: String,
        @SerializedName("pubnub_publish_key")
        val pubnubPublishKey: String?,
        @SerializedName("sendbird_app_id")
        val sendBirdAppId: String,
        @SerializedName("sendbird_api_endpoint")
        val sendBirdEndpoint: String,
        @SerializedName("programs_url")
        val programsUrl: String,
        @SerializedName("sessions_url")
        val sessionsUrl: String,
        @SerializedName("sticker_packs_url")
        val stickerPackUrl: String,
        @SerializedName("reaction_packs_url")
        val reactionPacksUrl: String,
        @SerializedName("mixpanel_token")
        val mixpanelToken: String,
        @SerializedName("analytics_properties")
        val analyticsProps: Map<String, String>,
        @SerializedName("chat_room_detail_url_template")
        val chatRoomDetailUrlTemplate: String,
        @SerializedName("create_chat_room_url")
        val createChatRoomUrl: String,
        @SerializedName("profile_url")
        val profileUrl: String,
        @SerializedName("profile_detail_url_template")
        val profileDetailUrlTemplate: String,
        @SerializedName("program_detail_url_template")
        val programDetailUrlTemplate: String,
        @SerializedName("pubnub_origin")
        val pubnubOrigin: String? = null,
        @SerializedName("leaderboard_detail_url_template")
        val leaderboardDetailUrlTemplate: String? = null,
        @SerializedName("pubnub_heartbeat_interval")
        val pubnubHeartbeatInterval: Int,
        @SerializedName("pubnub_presence_timeout")
        val pubnubPresenceTimeout: Int,
        @SerializedName("badges_url")
        val badgesUrl: String,
        @SerializedName("reward_items_url")
        internal var rewardItemsUrl: String?,
        @SerializedName("user_search_url")
        val userSearchUrl: String,
        @SerializedName("chat_rooms_invitations_url")
        val chatRoomsInvitationsUrl: String,
        @SerializedName("chat_room_invitation_detail_url_template")
        val chatRoomInvitationDetailUrlTemplate: String,
        @SerializedName("create_chat_room_invitation_url")
        val createChatRoomInvitationUrl: String,
        @SerializedName("profile_chat_room_received_invitations_url_template")
        val profileChatRoomReceivedInvitationsUrlTemplate: String,
        @SerializedName("profile_chat_room_sent_invitations_url_template")
        val profileChatRoomSentInvitationsUrlTemplate: String,
        @SerializedName("pinned_messages_url")
        val pinnedMessageUrl: String,
        @SerializedName("reward_transactions_url")
        val rewardTransactionsUrl: String = "",
        @SerializedName("sponsors_url")
        val sponsorsUrl: String?,
        @SerializedName("redemption_keys_url")
        val redemptionKeysUrl: String?,
        @SerializedName("redemption_key_detail_url_template")
        val redemptionKeyDetailUrlTemplate: String?,
        @SerializedName("redemption_key_detail_by_code_url_template")
        val redemptionKeyDetailByCodeUrlTemplate: String?
    )

    companion object {
        @JvmStatic
        var enableDebug: Boolean = false

        @JvmStatic
        var predictionWidgetVoteRepository: PredictionWidgetVoteRepository =
            LocalPredictionWidgetVoteRepository()
    }
}
