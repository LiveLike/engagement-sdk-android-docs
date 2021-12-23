package com.livelike.engagementsdk.sponsorship

import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.InternalLiveLikeChatClient
import com.livelike.engagementsdk.chat.LiveLikeChatClient
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
    private val chatClient: LiveLikeChatClient
) : ISponsor {

    override fun fetchByProgramId(
        programId: String,
        callback: LiveLikeCallback<List<SponsorModel>>
    ) {

        if (!validateUuid(programId)) {
            callback.onResponse(null, "invalid program ID")
            return
        }

        val programRepository = ProgramRepository(programId, userRepository)
        sdkScope.launch {
            configurationUserPairFlow.collect {
                val result = programRepository.getProgramData(it.second.programDetailUrlTemplate)
                if (result is Result.Success) {
                    callback.onResponse(result.data.sponsors, null)
                } else if (result is Result.Error) {
                    callback.onResponse(null, result.exception.message ?: "Error in fetching data")
                }
            }
        }
    }

    override fun fetchForApplication(
        callback: LiveLikeCallback<List<SponsorModel>>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                val sponsorsUrl = pair.second.sponsorsUrl
                if (sponsorsUrl != null) {
                    fetchSponsorDetails(
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

    private fun fetchSponsorDetails(
        sponsorUrl: String,
        accessToken: String,
        callback: LiveLikeCallback<List<SponsorModel>>
    ) {
        sdkScope.launch {
            val result = dataClient.remoteCall<SponsorListResponse>(
                sponsorUrl,
                RequestType.GET,
                accessToken = accessToken,
                fullErrorJson = true
            )
            if (result is Result.Success) {
                callback.onResponse(result.data.results, null)
            } else if (result is Result.Error) {
                callback.onResponse(
                    null,
                    result.exception.message ?: "Error in fetching data"
                )
            }
        }
    }

    override fun fetchByChatRoomId(
        chatRoomId: String,
        callback: LiveLikeCallback<List<SponsorModel>>
    ) {
        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                (chatClient as InternalLiveLikeChatClient).getChatRoom(
                    chatRoomId
                ).collect { result ->
                    if (result is Result.Success) {
                        result.data.sponsorsUrl?.let {
                            fetchSponsorDetails(it, pair.first.accessToken, callback)
                        }
                    } else if (result is Result.Error) {
                        callback.processResult(result)
                    }
                }
            }
        }
    }
}

data class SponsorListResponse(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<SponsorModel>
)