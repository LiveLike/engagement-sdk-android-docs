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
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.Queue
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getSharedAccessToken
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.initLiveLikeSharedPrefs
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.setSharedAccessToken
import com.livelike.engagementsdk.core.utils.map
import com.livelike.engagementsdk.gamification.Badges
import com.livelike.engagementsdk.gamification.IRewardsClient
import com.livelike.engagementsdk.gamification.Rewards
import com.livelike.engagementsdk.publicapis.*
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
        configurationStream.subscribe(this) { configuration ->
            configuration?.let {
                configurationStream.unsubscribe(this)
                dataClient.getProgramData(
                    configuration.programDetailUrlTemplate.replace(
                        TEMPLATE_PROGRAM_ID,
                        programId
                    )
                ) { program, error ->
                    when {
                        program?.leaderboards != null -> {
                            liveLikeCallback.onResponse(
                                program.leaderboards.map {
                                    LeaderBoard(
                                        it.id,
                                        it.name,
                                        it.rewardItem.toReward()
                                    )
                                },
                                null
                            )
                        }
                        error != null -> {
                            liveLikeCallback.onResponse(null, error)
                        }
                        else -> {
                            liveLikeCallback.onResponse(null, "Unable to fetch LeaderBoards")
                        }
                    }
                }
            }
        }
    }

    override fun getLeaderBoardDetails(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoard>
    ) {
        configurationStream.subscribe(this) {
            it?.let {
                configurationStream.unsubscribe(this)
                uiScope.launch {
                    val url = "${
                        it.leaderboardDetailUrlTemplate?.replace(
                            TEMPLATE_LEADER_BOARD_ID,
                            leaderBoardId
                        )
                    }"
                    val result = dataClient.remoteCall<LeaderBoardResource>(
                        url,
                        requestType = RequestType.GET,
                        accessToken = null
                    )
                    if (result is Result.Success) {
                        liveLikeCallback.onResponse(
                            result.data.toLeaderBoard(),
                            null
                        )
                        // leaderBoardDelegate?.leaderBoard(result.data.toLeadBoard(),result.data)
                    } else if (result is Result.Error) {
                        liveLikeCallback.onResponse(null, result.exception.message)
                    }
                }
            }
        }
    }

    override fun getLeaderboardClients(
        leaderBoardId: List<String>,
        liveLikeCallback: LiveLikeCallback<LeaderboardClient>
    ) {
        configurationStream.subscribe(this) {
            it?.let {
                userRepository.currentUserStream.subscribe(this) { user ->
                    userRepository.currentUserStream.unsubscribe(this)
                    configurationStream.unsubscribe(this)
                    CoroutineScope(Dispatchers.IO).launch {

                        val job = ArrayList<Job>()
                        for (i in 0 until leaderBoardId.size) {
                            job.add(
                                launch {
                                    val url = "${
                                        it.leaderboardDetailUrlTemplate?.replace(
                                            TEMPLATE_LEADER_BOARD_ID,
                                            leaderBoardId.get(i)
                                        )
                                    }"
                                    val result = dataClient.remoteCall<LeaderBoardResource>(
                                        url,
                                        requestType = RequestType.GET,
                                        accessToken = null
                                    )
                                    if (result is Result.Success) {
                                        user?.let { user ->
                                            val result2 =
                                                getLeaderBoardEntry(it, result.data.id, user.id)
                                            if (result2 is Result.Success) {
                                                leaderBoardDelegate?.leaderBoard(
                                                    LeaderBoardForClient(
                                                        result.data.id,
                                                        result.data.name,
                                                        result.data.rewardItem
                                                    ),
                                                    LeaderboardPlacement(
                                                        result2.data.rank,
                                                        result2.data.percentile_rank.toString(),
                                                        result2.data.score
                                                    )
                                                )
//                                            leaderBoardClientList.add(LeaderboardClient(result.data.id,result.data.name,result.data.rewardItem,LeaderboardPlacement(result2.data.rank
//                                            ,result2.data.percentile_rank.toString(),result2.data.score),leaderBoardDelegate!!))
                                                liveLikeCallback.onResponse(
                                                    LeaderboardClient(
                                                        result.data.id,
                                                        result.data.name,
                                                        result.data.rewardItem,
                                                        LeaderboardPlacement(
                                                            result2.data.rank,
                                                            result2.data.percentile_rank.toString(),
                                                            result2.data.score
                                                        ),
                                                        leaderBoardDelegate!!
                                                    ),
                                                    null
                                                )
                                            } else if (result2 is Result.Error) {
                                                leaderBoardDelegate?.leaderBoard(
                                                    LeaderBoardForClient(
                                                        result.data.id,
                                                        result.data.name,
                                                        result.data.rewardItem
                                                    ),
                                                    LeaderboardPlacement(0, " ", 0)
                                                )
                                            }
//
                                        }
                                    } else if (result is Result.Error) {
                                        liveLikeCallback.onResponse(null, result.exception.message)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
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

    override fun sponsor(): Sponsor {
        return Sponsor(this)
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

    private var leaderBoardEntryResult: HashMap<String, LeaderBoardEntryResult> = hashMapOf()
    private val leaderBoardEntryPaginationQueue =
        Queue<Pair<LiveLikePagination, Pair<String, LiveLikeCallback<LeaderBoardEntryPaginationResult>>>>()
    private var isQueueProcess = false

    override fun getEntriesForLeaderBoard(
        leaderBoardId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntryPaginationResult>
    ) {
        leaderBoardEntryPaginationQueue.enqueue(
            Pair(
                liveLikePagination,
                Pair(leaderBoardId, liveLikeCallback)
            )
        )
        if (!isQueueProcess) {
            val pair = leaderBoardEntryPaginationQueue.dequeue()
            if (pair != null)
                getEntries(pair)
        }
    }

    private fun getEntries(pair: Pair<LiveLikePagination, Pair<String, LiveLikeCallback<LeaderBoardEntryPaginationResult>>>) {
        isQueueProcess = true
        configurationStream.subscribe(this) { sdkConfiguration ->
            sdkConfiguration?.let {
                configurationStream.unsubscribe(this)
                uiScope.launch {
                    val leaderBoardId = pair.second.first
                    val liveLikeCallback = pair.second.second
                    val entriesUrl = when (pair.first) {
                        LiveLikePagination.FIRST -> {
                            val url = "${
                                it.leaderboardDetailUrlTemplate?.replace(
                                    TEMPLATE_LEADER_BOARD_ID,
                                    leaderBoardId
                                )
                            }"
                            val result = dataClient.remoteCall<LeaderBoardResource>(
                                url,
                                requestType = RequestType.GET,
                                accessToken = null
                            )
                            var defaultUrl = ""
                            if (result is Result.Success) {
                                defaultUrl = result.data.entries_url
                            } else if (result is Result.Error) {
                                defaultUrl = ""
                                liveLikeCallback.onResponse(null, result.exception.message)
                            }
                            defaultUrl
                        }
                        LiveLikePagination.NEXT -> leaderBoardEntryResult[leaderBoardId]?.next
                        LiveLikePagination.PREVIOUS -> leaderBoardEntryResult[leaderBoardId]?.previous
                    }
                    if (entriesUrl != null && entriesUrl.isNotEmpty()) {
                        val listResult = dataClient.remoteCall<LeaderBoardEntryResult>(
                            entriesUrl,
                            requestType = RequestType.GET,
                            accessToken = null
                        )
                        if (listResult is Result.Success) {
                            leaderBoardEntryResult[leaderBoardId] = listResult.data
                            liveLikeCallback.onResponse(
                                leaderBoardEntryResult[leaderBoardId]?.let {
                                    LeaderBoardEntryPaginationResult(
                                        it.count ?: 0,
                                        it.previous != null,
                                        it.next != null,
                                        it.results
                                    )
                                },
                                null
                            )
                        } else if (listResult is Result.Error) {
                            liveLikeCallback.onResponse(
                                null,
                                listResult.exception.message
                            )
                        }
                        isQueueProcess = false
                        val dequeuePair = leaderBoardEntryPaginationQueue.dequeue()
                        if (dequeuePair != null)
                            getEntries(dequeuePair)
                    } else if (entriesUrl == null || entriesUrl.isEmpty()) {
                        liveLikeCallback.onResponse(null, "No More data to load")
                        isQueueProcess = false
                        val dequeuePair = leaderBoardEntryPaginationQueue.dequeue()
                        if (dequeuePair != null)
                            getEntries(dequeuePair)
                    }
                }
            }
        }
    }

    override fun getLeaderBoardEntryForProfile(
        leaderBoardId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    ) {
        configurationStream.subscribe(this) {
            it?.let {
                configurationStream.unsubscribe(this)
                uiScope.launch {
                    val url = "${
                        it.leaderboardDetailUrlTemplate?.replace(
                            TEMPLATE_LEADER_BOARD_ID,
                            leaderBoardId
                        )
                    }"
                    val result = dataClient.remoteCall<LeaderBoardResource>(
                        url,
                        requestType = RequestType.GET,
                        accessToken = null
                    )
                    if (result is Result.Success) {
                        val profileResult = dataClient.remoteCall<LeaderBoardEntry>(
                            result.data.entry_detail_url_template.replace(
                                TEMPLATE_PROFILE_ID,
                                profileId
                            ),
                            requestType = RequestType.GET,
                            accessToken = null
                        )
                        if (profileResult is Result.Success) {
                            liveLikeCallback.onResponse(profileResult.data, null)
                            // leaderBoardDelegate?.leaderBoard(profileResul)
                        } else if (profileResult is Result.Error) {
                            liveLikeCallback.onResponse(null, profileResult.exception.message)
                        }
                    } else if (result is Result.Error) {
                        liveLikeCallback.onResponse(null, result.exception.message)
                    }
                }
            }
        }

        getLeaderBoardDetails(
            leaderBoardId,
            object : LiveLikeCallback<LeaderBoard>() {
                override fun onResponse(result: LeaderBoard?, error: String?) {
                    result?.let {
                        uiScope.launch {
                        }
                    }
                    error?.let {
                        liveLikeCallback.onResponse(null, error)
                    }
                }
            }
        )
    }

    internal suspend fun getLeaderBoardEntry(
        sdkConfig: SdkConfiguration,
        leaderBoardId: String,
        profileId: String
    ): Result<LeaderBoardEntry> {
        val url = "${
            sdkConfig.leaderboardDetailUrlTemplate?.replace(
                TEMPLATE_LEADER_BOARD_ID,
                leaderBoardId
            )
        }"
        val result = dataClient.remoteCall<LeaderBoardResource>(
            url,
            requestType = RequestType.GET,
            accessToken = null
        )
        return when (result) {
            is Result.Success -> {
                dataClient.remoteCall(
                    result.data.entry_detail_url_template.replace(
                        TEMPLATE_PROFILE_ID,
                        profileId
                    ),
                    requestType = RequestType.GET,
                    accessToken = null
                )
            }
            is Result.Error -> {
                Result.Error(result.exception)
            }
        }
    }

    override fun getLeaderBoardEntryForCurrentUserProfile(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    ) {
        userRepository.currentUserStream.subscribe(this) {
            it?.let { user ->
                userRepository.currentUserStream.unsubscribe(this)
                getLeaderBoardEntryForProfile(leaderBoardId, user.id, liveLikeCallback)
            }
        }
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
            chat()
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
        val pinnedMessageUrl: String
    )

    companion object {
        @JvmStatic
        var enableDebug: Boolean = false

        @JvmStatic
        var predictionWidgetVoteRepository: PredictionWidgetVoteRepository =
            LocalPredictionWidgetVoteRepository()
    }
}
