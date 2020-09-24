package com.livelike.engagementsdk.widget.domain

import com.livelike.engagementsdk.core.data.models.LeaderBoard
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntry
import com.livelike.engagementsdk.core.data.models.LeaderBoardResource
import com.livelike.engagementsdk.core.data.models.LeaderboardPlacement


interface LeaderBoardDelegate {
    fun leaderBoard(leaderBoard : LeaderBoard, currentUserPlacementDidChange : LeaderBoardEntry)
}


data class LeaderBoardUserDetails(
    val leaderBoard: LeaderBoard,val currentUserPlacementDidChange: LeaderBoardEntry
)
