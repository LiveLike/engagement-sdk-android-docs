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
    private val configurationUserPairFlow: Flow<Pair<LiveLikeUser, EngagementSDK.SdkConfiguration>>,
    private val dataClient: EngagementDataClientImpl,
    private val sdkScope: CoroutineScope
) : IRewardsClient {

    private var lastRewardItemsPage: LLPaginatedResult<RewardItem>? = null

    private var lastRewardTransfersPage: LLPaginatedResult<TransferRewardItem>? = null

    override fun getApplicationRewardItems(
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
                                pair.first.accessToken
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
        recipientProfileId: String,
        liveLikeCallback: LiveLikeCallback<TransferRewardItem>
    ) {

        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                pair.first?.let {
                    it.rewardItemTransferUrl?.let { url ->

                        val body = gson.toJson(
                            TransferRewardItemRequest(
                                recipientProfileId,
                                amount,
                                rewardItemId
                            )
                        )
                            .toRequestBody("application/json".toMediaTypeOrNull())

                        dataClient.remoteCall<TransferRewardItem>(
                            url,
                            RequestType.POST,
                            body,
                            pair.first.accessToken
                        ).run {
                            liveLikeCallback.processResult(this)
                        }
                    }
                }
            }
        }
    }

    override fun getRewardItemTransfers(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<TransferRewardItem>>
    ) {
        var fetchUrl: String? = null

        sdkScope.launch {
            if (lastRewardTransfersPage == null || liveLikePagination == LiveLikePagination.FIRST) {
                configurationUserPairFlow.collect { pair ->
                    pair.first?.let {
                        fetchUrl = it.rewardItemTransferUrl
                    }
                }
            } else {
                fetchUrl = lastRewardTransfersPage?.getPaginationUrl(liveLikePagination)
            }

            if (fetchUrl == null) {
                liveLikeCallback.onResponse(null, "No more data")
            } else {
                configurationUserPairFlow.collect { pair ->
                    dataClient.remoteCall<LLPaginatedResult<TransferRewardItem>>(
                        fetchUrl ?: "",
                        RequestType.GET,
                        null,
                        pair.first.accessToken
                    ).run {
                        if (this is Result.Success) {
                            lastRewardTransfersPage = this.data
                        }
                        liveLikeCallback.processResult(this)
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
    fun getApplicationRewardItems(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<RewardItem>>
    )

    /**
     * fetch all the current user's balance for the passed rewardItemIDs
     * in callback you will receive a map of rewardItemId and balance
     **/
    fun getRewardItemsBalance(
        rewardItemIds: List<String>,
        liveLikeCallback: LiveLikeCallback<Map<String, Int>>
    )

    /**
     * transfer specified reward item amount to the profileID
     * amount should be a positive whole number
     **/
    fun transferAmountToProfileId(
        rewardItemId: String,
        amount: Int,
        recipientProfileId: String,
        liveLikeCallback: LiveLikeCallback<TransferRewardItem>
    )

    /**
     * Retrieve all reward item transfers associated
     * with the current user profile
     **/
    fun getRewardItemTransfers(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<TransferRewardItem>>
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
    @SerializedName("recipient_profile_id")
    val receiverProfileId: String,
    @SerializedName("reward_item_amount")
    val rewardItemAmount: Int,
    @SerializedName("reward_item_id")
    val rewardItemId: String
)

data class TransferRewardItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("recipient_profile_id")
    val recipientProfileId: String,
    @SerializedName("reward_item_amount")
    val rewardItemAmount: Int,
    @SerializedName("reward_item_id")
    val rewardItemId: String,
    @SerializedName("sender_profile_id")
    val senderProfileId: String
)
