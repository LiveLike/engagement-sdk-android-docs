package com.livelike.engagementsdk.widget.domain

interface UserProfileDelegate {
    fun userProfile(reward : com.livelike.engagementsdk.widget.domain.Reward, rewardSource: RewardSource)
}


enum class RewardSource{
    WIDGETS
}


data class Reward(
//  TODO change to  val rewardItem: RewardItem,
    val rewardItem: String,
    val amount: Int
)
