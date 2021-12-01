package com.livelike.engagementsdk.gamification

/**
 * all events related to rewards like reward transfer received will be surfaced onto this listener
 * integrators can register this listener on a instance of rewardClient that is equivalent to sdk.rewards()
 **/
abstract class RewardEventsListener {

    /**
     * This is executed whenever the user receives a reward transfer
     * @param rewardItemTransfer this is the Reward Item transfer payload representing the transfer received by user
     **/
    open fun onReceiveNewRewardItemTransfer(
        rewardItemTransfer: TransferRewardItem
    ){
        return
    }
}


internal enum class RewardEvent(val key: String) {
    REWARD_ITEM_TRANSFER_RECEIVED("reward-item-transfer-received");
}