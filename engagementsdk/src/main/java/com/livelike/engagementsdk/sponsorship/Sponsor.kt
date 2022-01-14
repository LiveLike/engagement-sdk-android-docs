package com.livelike.engagementsdk.sponsorship

import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.InternalLiveLikeChatClient
import com.livelike.engagementsdk.chat.LiveLikeChatClient
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.validateUuid
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Sponsor client allowing to fetch all sponsor related stuff from Livelike's CMS
 */

internal class Sponsor(
    private val configurationUserPairFlow: Flow<Pair<LiveLikeUser, EngagementSDK.SdkConfiguration>>,
    private var dataClient: EngagementDataClientImpl,
    private val sdkScope: CoroutineScope,
    private val userRepository: UserRepository,
    private val chatClient: LiveLikeChatClient,
    private val uiScope: CoroutineScope
) : ISponsor {

    private val sponsorTypeListResponseMap = hashMapOf<SponsorListType, SponsorListResponse>()

    override fun fetchByProgramId(
        programId: String,
        pagination: LiveLikePagination,
        callback: LiveLikeCallback<List<SponsorModel>>
    ) {

        if (!validateUuid(programId)) {
            callback.onResponse(null, "invalid program ID")
            return
        }

        val programRepository = ProgramRepository(programId, userRepository)
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                val result = programRepository.getProgramData(pair.second.programDetailUrlTemplate)
                uiScope.launch {
                    if (result is Result.Success) {
                        if (result.data.sponsorsUrl != null) {
                            fetchSponsorDetails(
                                SponsorListType.Program,
                                pagination,
                                result.data.sponsorsUrl,
                                pair.first.accessToken,
                                callback
                            )
                        } else {
                            callback.onResponse(null, "Error in fetching data")
                        }
                    } else if (result is Result.Error) {
                        callback.onResponse(
                            null,
                            result.exception.message ?: "Error in fetching data"
                        )
                    }
                }
            }
        }
    }

    override fun fetchForApplication(
        pagination: LiveLikePagination,
        callback: LiveLikeCallback<List<SponsorModel>>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                val sponsorsUrl = pair.second.sponsorsUrl
                uiScope.launch {
                    if (sponsorsUrl != null) {
                        fetchSponsorDetails(
                            SponsorListType.Application,
                            pagination,
                            pair.second.sponsorsUrl!!,
                            pair.first.accessToken,
                            callback
                        )
                    } else {
                        callback.onResponse(null, "Error in fetching data")
                    }
                }
            }
        }
    }

    private fun fetchSponsorDetails(
        sponsorListType: SponsorListType,
        liveLikePagination: LiveLikePagination,
        sponsorUrl: String,
        accessToken: String,
        callback: LiveLikeCallback<List<SponsorModel>>
    ) {
        sdkScope.launch {
            val response = sponsorTypeListResponseMap[sponsorListType]
            val url = when (liveLikePagination) {
                LiveLikePagination.FIRST -> sponsorUrl
                LiveLikePagination.NEXT -> response?.next
                LiveLikePagination.PREVIOUS -> response?.previous
            }
            if (url != null) {
                val result = dataClient.remoteCall<SponsorListResponse>(
                    url,
                    RequestType.GET,
                    accessToken = accessToken,
                    fullErrorJson = true
                )
                if (result is Result.Success) {
                    sponsorTypeListResponseMap[sponsorListType] = result.data
                    callback.onResponse(result.data.results, null)
                } else if (result is Result.Error) {
                    callback.onResponse(
                        null,
                        result.exception.message ?: "Error in fetching data"
                    )
                }
            } else {
                callback.onResponse(null, "No More data to load")
            }
        }
    }

    override fun fetchByChatRoomId(
        chatRoomId: String,
        pagination: LiveLikePagination,
        callback: LiveLikeCallback<List<SponsorModel>>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                (chatClient as InternalLiveLikeChatClient).getChatRoom(
                    chatRoomId
                ).collect { result ->
                    uiScope.launch {
                        if (result is Result.Success) {
                            result.data.sponsorsUrl?.let {
                                fetchSponsorDetails(
                                    SponsorListType.ChatRoom,
                                    pagination,
                                    it,
                                    pair.first.accessToken,
                                    callback
                                )
                            }
                        } else if (result is Result.Error) {
                            callback.processResult(result)
                        }
                    }
                }
            }
        }
    }
}

internal enum class SponsorListType { Program, Application, ChatRoom }

data class SponsorListResponse(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<SponsorModel>
)