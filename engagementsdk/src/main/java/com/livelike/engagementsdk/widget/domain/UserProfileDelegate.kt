package com.livelike.engagementsdk.widget.domain

import com.livelike.engagementsdk.core.data.models.RewardItem

abstract class UserProfileDelegate {

    abstract fun userProfile(reward : com.livelike.engagementsdk.widget.domain.Reward, rewardSource: RewardSource)
}


enum class RewardSource{
    WIDGETS
}


data class Reward(
    val rewardItem: RewardItem,
    val amount: Int
)
