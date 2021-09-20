package com.livelike.engagementsdk.gamification

import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LLPaginatedResult
import com.livelike.engagementsdk.core.data.models.RewardItem
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


internal class Rewards(
    val configurationUserPairFlow: Flow<Pair<LiveLikeUser, EngagementSDK.SdkConfiguration>>,
    val dataClient: EngagementDataClientImpl,
    val sdkScope: CoroutineScope
) : IRewardsClient {

    private var lastRewardItemsPage: LLPaginatedResult<RewardItem>? = null

    override fun getAllRewards(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<RewardItem>>
    ) {
        var fetchUrl: String? = null

        sdkScope.launch {
            if (lastRewardItemsPage == null || liveLikePagination == LiveLikePagination.FIRST) {
                configurationUserPairFlow.collect { pair ->
                    pair.second?.let {
                        fetchUrl = it.rewardItemsUrl
                    }
                }
            } else {
                fetchUrl = lastRewardItemsPage?.getPaginationUrl(liveLikePagination)
            }

            if (fetchUrl == null) {
                liveLikeCallback.onResponse(null, "No more data")
            } else {
                dataClient.remoteCall<LLPaginatedResult<RewardItem>>(
                    fetchUrl ?: "",
                    RequestType.GET,
                    null,
                    null
                ).run {
                    if (this is Result.Success) {
                        lastRewardItemsPage = this.data
                    }
                    liveLikeCallback.processResult(this)
                }
            }
        }
    }

    override fun getRewardItemsBalance(
        rewardItemIds: List<String>,
        liveLikeCallback: LiveLikeCallback<Map<String, Int>>
    ) {
        TODO("Not yet implemented")
    }

    override fun transferAmountToProfileId(
        rewardItemId: String,
        amount: Int,
        receiverProfileId: String,
        liveLikeCallback: LiveLikeCallback<TransferRewardItemResponse>
    ) {
        TODO("Not yet implemented")
    }


}


/**
All the apis related to rewards item discovery, balance and transfer exposed here
*/
interface IRewardsClient {

    /**
     * fetch all the rewards item associated to the client id passed at initialization of sdk
     * to fetch next page function need to be called again with LiveLikePagination.NEXT and for first call as LiveLikePagination.FIRST
     **/
    fun getAllRewards(liveLikePagination: LiveLikePagination, liveLikeCallback: LiveLikeCallback<LLPaginatedResult<RewardItem>>)


    fun getRewardItemsBalance(
        rewardItemIds: List<String>,
        liveLikeCallback: LiveLikeCallback<Map<String, Int>>
    )

    fun transferAmountToProfileId(
        rewardItemId: String,
        amount: Int,
        receiverProfileId: String,
        liveLikeCallback: LiveLikeCallback<TransferRewardItemResponse>
    )
}


class TransferRewardItemResponse{

}

