package com.livelike.engagementsdk.data.repository

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.data.models.ProgramRank
import com.livelike.engagementsdk.services.network.Program
import com.livelike.engagementsdk.services.network.RequestType
import com.livelike.engagementsdk.services.network.Result
import com.livelike.engagementsdk.utils.SubscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that handles program and program-user data.
 * Program is an event in CMS App.
 */
internal class ProgramRepository(
    private val programId: String,
    private val userRepository: UserRepository
) : BaseRepository() {

    lateinit var program: Program
    /**
     *  user points and other gamification stuff under this program.
     */
    val programRankStream: Stream<ProgramRank> = SubscriptionManager()

    suspend fun fetchProgramRank() {

        val result = dataClient.remoteCall<ProgramRank>(
            program.rankUrl,
            RequestType.GET,
            accessToken = userRepository.userAccessToken
        )
        if (result is Result.Success) {
            withContext(Dispatchers.Main) {
                programRankStream.onNext(result.data as ProgramRank)
            }
        }
    }
}
