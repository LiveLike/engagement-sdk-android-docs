package com.livelike.engagementsdk.gamification

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LLPaginatedResult
import com.livelike.engagementsdk.core.data.models.RewardItem
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

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

        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                pair.first?.let {
                    it.rewardItemBalancesUrl?.let { url ->
                        val httpUrl = url.toHttpUrlOrNull()?.newBuilder()?.apply {
                            for (id in rewardItemIds) {
                                addQueryParameter("reward_item_id", id)
                            }
                        }
                        httpUrl?.build()?.let { httpUrl ->
                            dataClient.remoteCall<RewardItemBalancesApiResponse>(
                                httpUrl,
                                RequestType.GET,
                                null,
                                null
                            ).run {
                                if (this is Result.Success) {
                                    liveLikeCallback.onResponse(
                                        this.data.rewardItemBalanceList.associate { rewardItemBalance ->
                                            Pair(
                                                rewardItemBalance.rewardItemId,
                                                rewardItemBalance.rewardItemBalance
                                            )
                                        },
                                        null
                                    )
                                } else if (this is Result.Error) {
                                    liveLikeCallback.onResponse(
                                        null,
                                        this.exception.message ?: "Error in fetching data"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun transferAmountToProfileId(
        rewardItemId: String,
        amount: Int,
        receiverProfileId: String,
        liveLikeCallback: LiveLikeCallback<TransferRewardItemResponse>
    ) {

        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                pair.first?.let {
                    it.rewardItemTransferUrl?.let { url ->

                        val body = gson.toJson(TransferRewardItemRequest(receiverProfileId, amount, rewardItemId))
                            .toRequestBody("application/json".toMediaTypeOrNull())

                        dataClient.remoteCall<TransferRewardItemResponse>(
                            url,
                            RequestType.GET,
                            body,
                            null
                        ).run {
                            liveLikeCallback.processResult(this)
                        }
                    }
                }
            }
        }
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
    fun getAllRewards(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<RewardItem>>
    )

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

internal data class RewardItemBalancesApiResponse(
    @SerializedName("reward_item_balances")
    val rewardItemBalanceList: List<RewardItemBalance>
)

internal data class RewardItemBalance(
    @SerializedName("reward_item_balance")
    val rewardItemBalance: Int,
    @SerializedName("reward_item_id")
    val rewardItemId: String,
    @SerializedName("reward_item_name")
    val rewardItemName: String
)

internal data class TransferRewardItemRequest(
    val receiver_profile_id: String,
    val reward_item_amount: Int,
    val reward_item_id: String
)

data class TransferRewardItemResponse(
    val created_at: String,
    val new_balance: Int,
    val previous_balance: Int,
    val recipient_new_balance: Int
)
