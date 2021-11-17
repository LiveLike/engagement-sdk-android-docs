package com.livelike.engagementsdk.gamification

import com.livelike.engagementsdk.*
import com.livelike.engagementsdk.TEMPLATE_LEADER_BOARD_ID
import com.livelike.engagementsdk.TEMPLATE_PROFILE_ID
import com.livelike.engagementsdk.TEMPLATE_PROGRAM_ID
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.*
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.utils.Queue
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.livelike.engagementsdk.core.services.network.Result

/**
 * LeaderBoard client allowing to fetch all leaderboard related stuff
 */

internal class InternalLiveLikeLeaderBoardClient(
    private val configurationStream: Stream<EngagementSDK.SdkConfiguration>,
    private val userRepository: UserRepository,
    private val uiScope: CoroutineScope,
    private val dataClient: EngagementDataClientImpl,): LiveLikeLeaderBoardClient {

    private var leaderBoardEntryResult: HashMap<String, LeaderBoardEntryResult> = hashMapOf()
    private val leaderBoardEntryPaginationQueue =
        Queue<Pair<LiveLikePagination, Pair<String, LiveLikeCallback<LeaderBoardEntryPaginationResult>>>>()
    private var isQueueProcess = false


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
                            result.data.toLeadBoard(),
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
                        for (i in leaderBoardId.indices) {
                            job.add(
                                launch {
                                    val url = "${
                                        it.leaderboardDetailUrlTemplate?.replace(
                                            TEMPLATE_LEADER_BOARD_ID,
                                            leaderBoardId[i]
                                        )
                                    }"
                                    val result = dataClient.remoteCall<LeaderBoardResource>(
                                        url,
                                        requestType = RequestType.GET,
                                        accessToken = null
                                    )
                                    if (result is Result.Success) {
                                        user?.let { user ->
                                            getLeaderBoardEntryOnSuccess(
                                                it,
                                                result,
                                                user,
                                                liveLikeCallback
                                            )
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

    private suspend fun getLeaderBoardEntryOnSuccess(
        it: EngagementSDK.SdkConfiguration,
        result: Result.Success<LeaderBoardResource>,
        user: LiveLikeUser,
        liveLikeCallback: LiveLikeCallback<LeaderboardClient>
    ) {
        val result2 =
            getLeaderBoardEntry(it, result.data.id, user.id)
        if (result2 is Result.Success) {
            userRepository.leaderBoardDelegate?.leaderBoard(
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
                    userRepository.leaderBoardDelegate!!
                ),
                null
            )
        } else if (result2 is Result.Error) {
            userRepository.leaderBoardDelegate?.leaderBoard(
                LeaderBoardForClient(
                    result.data.id,
                    result.data.name,
                    result.data.rewardItem
                ),
                LeaderboardPlacement(0, " ", 0)
            )
        }
    }


    private suspend fun getLeaderBoardEntry(
        sdkConfig: EngagementSDK.SdkConfiguration,
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
}