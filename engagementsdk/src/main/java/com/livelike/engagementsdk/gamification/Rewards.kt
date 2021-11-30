package com.livelike.engagementsdk.gamification

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LLPaginatedResult
import com.livelike.engagementsdk.core.data.models.RewardItem
import com.livelike.engagementsdk.core.exceptionhelpers.safeCodeBlockCall
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.services.messaging.LiveLikeEventMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.ref.WeakReference

internal class Rewards(
    private val configurationUserPairFlow: Flow<Pair<LiveLikeUser, EngagementSDK.SdkConfiguration>>,
    private val dataClient: EngagementDataClientImpl,
    private val sdkScope: CoroutineScope
) : IRewardsClient {

    private var lastRewardItemsPage: LLPaginatedResult<RewardItem>? = null

    /*map of rewardITemRequestOptions and last response*/
    private var lastRewardTransfersPageMap: MutableMap<RewardItemTransferRequestParams,LLPaginatedResult<TransferRewardItem>?> = mutableMapOf()

    private var rewardTransactionsPageMap: MutableMap<RewardTransactionsRequestParameters,LLPaginatedResult<RewardTransaction>?> = mutableMapOf()

    private var _rewardEventsListener: WeakReference<RewardEventsListener>? = null
    override var rewardEventsListener: RewardEventsListener?
        set(value) {
            value?.let {
                _rewardEventsListener = WeakReference(value)
                subscribeToRewardEvents()
            } ?: run {
                unsubscribeToRewardEvents()
                _rewardEventsListener = null
            }
        }
        get() {
            return _rewardEventsListener?.get()
        }

    private fun subscribeToRewardEvents() {
        sdkScope.launch {
            configurationUserPairFlow.collect {
                LiveLikeEventMessagingService.subscribeWidgetChannel(
                    it.first.subscribeChannel ?: "",
                    this@Rewards,
                    it.second,
                    it.first
                ) { event ->
                    _rewardEventsListener?.get()?.let { listener ->
                        event?.let {
                            safeCodeBlockCall({
                                val eventType = event.message.get("event").asString ?: ""
                                val payload = event.message["payload"].asJsonObject
                                if (eventType == RewardEvent.REWARD_ITEM_TRANSFER_RECEIVED.key) {
                                    listener.onReceiveNewRewardItemTransfer(
                                        gson.fromJson(
                                            payload.toString(),
                                            TransferRewardItem::class.java
                                        )
                                    )
                                }
                            })
                        }
                        listener
                    }?: run {
                        //weak ref to listener has returned null we need auto unsubscribed
                        unsubscribeToRewardEvents()
                        _rewardEventsListener = null
                    }
                }
            }
        }
    }

    private fun unsubscribeToRewardEvents() {
        sdkScope.launch {
            configurationUserPairFlow.collect {
                LiveLikeEventMessagingService.unsubscribeWidgetChannel(
                    it.first.subscribeChannel ?: "", this@Rewards
                )
            }
        }
    }

    override fun getApplicationRewardItems(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<RewardItem>>
    ) {
        var fetchUrl: String? = null

        sdkScope.launch {
            if (lastRewardItemsPage == null || liveLikePagination == LiveLikePagination.FIRST) {
                configurationUserPairFlow.collect { pair ->
                    fetchUrl = pair.second.rewardItemsUrl
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

    override fun getRewardItemBalances(
        liveLikePagination: LiveLikePagination,
        rewardItemIds: List<String>,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<RewardItemBalance>>
    ) {

        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                pair.first.rewardItemBalancesUrl?.let { url ->
                    val httpUrl = url.toHttpUrlOrNull()?.newBuilder()?.apply {
                        for (id in rewardItemIds) {
                            addQueryParameter("reward_item_id", id)
                        }
                    }
                    httpUrl?.build()?.let { innerHttpUrl ->
                        dataClient.remoteCall<LLPaginatedResult<RewardItemBalance>>(
                            innerHttpUrl,
                            RequestType.GET,
                            null,
                            pair.first.accessToken
                        ).run {
                            if (this is Result.Success) {
                                liveLikeCallback.onResponse(
                                    this.data,
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

    override fun transferAmountToProfileId(
        rewardItemId: String,
        amount: Int,
        recipientProfileId: String,
        liveLikeCallback: LiveLikeCallback<TransferRewardItem>
    ) {

        sdkScope.launch {
            configurationUserPairFlow.collect { pair ->
                pair.first.rewardItemTransferUrl?.let { url ->

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

    override fun getRewardItemTransfers(
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<TransferRewardItem>>
    ) {
        return getRewardItemTransfers(
            liveLikePagination,
            RewardItemTransferRequestParams(null), liveLikeCallback
        )
    }

    override fun getRewardItemTransfers(
        liveLikePagination: LiveLikePagination,
        requestParams: RewardItemTransferRequestParams,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<TransferRewardItem>>
    ) {

        var fetchUrl: String? = null

        sdkScope.launch {
            if (lastRewardTransfersPageMap[requestParams] == null || liveLikePagination == LiveLikePagination.FIRST) {
                configurationUserPairFlow.collect { pair ->
                    fetchUrl = pair.first.rewardItemTransferUrl
                }
            } else {
                fetchUrl =
                    lastRewardTransfersPageMap[requestParams]?.getPaginationUrl(liveLikePagination)
            }

            if (fetchUrl == null) {
                liveLikeCallback.onResponse(null, "No more data")
            } else {
                configurationUserPairFlow.collect { pair ->
                    requestParams.transferType?.let {
                        fetchUrl = fetchUrl?.toHttpUrlOrNull()?.newBuilder()?.apply {
                            addQueryParameter("transfer_type", requestParams.transferType.key)
                        }?.build()?.toUrl()?.toString()
                    }
                    dataClient.remoteCall<LLPaginatedResult<TransferRewardItem>>(
                        fetchUrl ?: "",
                        RequestType.GET,
                        null,
                        pair.first.accessToken
                    ).run {
                        if (this is Result.Success) {
                            lastRewardTransfersPageMap[requestParams] = this.data
                        }
                        liveLikeCallback.processResult(this)
                    }
                }
            }
        }
    }

    override fun getRewardTransactions(
        liveLikePagination: LiveLikePagination,
        requestParams: RewardTransactionsRequestParameters,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<RewardTransaction>>
    ) {
        var fetchUrl: String? = null
        sdkScope.launch {
            if (rewardTransactionsPageMap[requestParams] == null || liveLikePagination == LiveLikePagination.FIRST) {
                configurationUserPairFlow.collect { pair ->
                    fetchUrl = pair.second.rewardTransactionsUrl
                }
            } else {
                fetchUrl =
                    rewardTransactionsPageMap[requestParams]?.getPaginationUrl(liveLikePagination)
            }
            if (fetchUrl == null) {
                liveLikeCallback.onResponse(null, "No more data")
            } else {
                configurationUserPairFlow.collect { pair ->
                    fetchUrl = fetchUrl?.toHttpUrlOrNull()?.newBuilder()?.apply {
                        requestParams.widgetIds.forEach {
                            addQueryParameter("widget_id", it)
                        }
                        requestParams.widgetKindFilter.forEach {
                            addQueryParameter("widget_kind", it.getType())
                        }
                    }?.build()?.toUrl()?.toString()

                    dataClient.remoteCall<LLPaginatedResult<RewardTransaction>>(
                        fetchUrl ?: "",
                        RequestType.GET,
                        null,
                        pair.first.accessToken
                    ).run {
                        if (this is Result.Success) {
                            rewardTransactionsPageMap[requestParams] = this.data
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

    var rewardEventsListener: RewardEventsListener?

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
    fun getRewardItemBalances(
        liveLikePagination: LiveLikePagination,
        rewardItemIds: List<String>,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<RewardItemBalance>>
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

    /**
     * Retrieve all reward item transfers associated
     * with the current user profile
     * @param requestParams allows to filter transfer based on transferType i.e sent or received
     **/
    fun getRewardItemTransfers(
        liveLikePagination: LiveLikePagination,
        requestParams : RewardItemTransferRequestParams,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<TransferRewardItem>>
    )

    /**
     * Retrieve all reward transactions associated
     * with the current user profile
     * @param requestParams allows to filter reward transactions based on widget id or widget kind
     **/
    fun getRewardTransactions(
        liveLikePagination: LiveLikePagination,
        requestParams: RewardTransactionsRequestParameters,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<RewardTransaction>>
    )

}

data class RewardTransaction (
    @SerializedName("id")
    val id: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("widget_kind")
    val widgetKind: String,
    @SerializedName("widget_id")
    val widgetId: String,
    @SerializedName("reward_item_id")
    val rewardItemId: String,
    @SerializedName("reward_item_name")
    val rewardItemName: String,
    @SerializedName("reward_action_id")
    val rewardActionId: String,
    @SerializedName("reward_action_key")
    val rewardActionKey: String,
    @SerializedName("reward_action_name")
    val rewardActionName: String,
    @SerializedName("profile_id")
    val profileId: String,
    @SerializedName("profile_nickname")
    val profileNickname: String,
    @SerializedName("reward_item_amount")
    val rewardItemAmount: String
)

data class RewardTransactionsRequestParameters(
    val widgetIds: Set<String> = emptySet(),
    val widgetKindFilter: Set<WidgetType> = emptySet()
)

@Deprecated("use paginates result class")
internal data class RewardItemBalancesApiResponse(
    @SerializedName("reward_item_balances")
    val rewardItemBalanceList: List<RewardItemBalance>
)

class RewardItemTransferRequestParams(internal val transferType: RewardItemTransferType?){

    override fun hashCode(): Int {
        return transferType?.key?.hashCode() ?: 0
    }

    override fun equals(other: Any?): Boolean {
        return transferType?.equals(other) ?: true
    }
}


enum class RewardItemTransferType(val key: String) {
    SENT("sent"),
    RECEIVED("received")
}

data class RewardItemBalance(
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
