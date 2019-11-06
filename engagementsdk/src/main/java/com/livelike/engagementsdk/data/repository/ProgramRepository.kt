package com.livelike.engagementsdk.data.repository

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.data.models.Program
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.data.models.RewardsType
import com.livelike.engagementsdk.services.network.RequestType
import com.livelike.engagementsdk.services.network.Result
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.logDebug
import com.livelike.engagementsdk.utils.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that handles program and program-user data.
 * Program is an event in CMS App.
 */
internal class ProgramRepository(
    val programId: String,
    private val userRepository: UserRepository
) : BaseRepository() {

    lateinit var program: Program

    val rewardType: RewardsType by lazy { RewardsType.valueOf(program.rewardsType.toUpperCase()) }
    /**
     *  user points and other gamification stuff under this program.
     */
    val programGamificationProfileStream: Stream<ProgramGamificationProfile> = SubscriptionManager()

    suspend fun fetchProgramRank() {
        if (program.rewardsType.equals(RewardsType.NONE.key)) {
            logError { "Should not call if Gamification is disabled" }
            return
        }

        val result = dataClient.remoteCall<ProgramGamificationProfile>(
            program.rankUrl,
            RequestType.GET,
            accessToken = userRepository.userAccessToken
        )
        if (result is Result.Success) {
            withContext(Dispatchers.Main) {
                val programGamification = result.data
                logDebug { "points update : ${programGamification.points}, rank update: ${programGamification.rank}" }
                programGamificationProfileStream.onNext(programGamification)
            }
        }
    }
}
