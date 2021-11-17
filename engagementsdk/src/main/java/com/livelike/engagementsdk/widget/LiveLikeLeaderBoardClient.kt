package com.livelike.engagementsdk.widget

import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LeaderBoard
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntry
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntryPaginationResult
import com.livelike.engagementsdk.core.data.models.LeaderboardClient
import com.livelike.engagementsdk.publicapis.LiveLikeCallback


interface LiveLikeLeaderBoardClient {

    /**
     * Fetch leaderboard associated with a specific program
     *
     * @param programId : id of the program for which leaderboards needed
     **/
    fun getLeaderBoardsForProgram(
        programId: String,
        liveLikeCallback: LiveLikeCallback<List<LeaderBoard>>
    )

    /**
     * Fetch leaderboard details via leaderboard id
     *
     * @param leaderBoardId : id of the leaderboard for which details needed
     **/
    fun getLeaderBoardDetails(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoard>
    )

    /**
     * Get all entries of a leaderboard with pagination
     *
     * @param leaderBoardId : id of the leaderboard for which details needed
     * @param liveLikePagination : pagination for the entries
     **/
    fun getEntriesForLeaderBoard(
        leaderBoardId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntryPaginationResult>
    )

    /**
     * Get leaderboard entries of profile with leaderboard id
     *
     * @param leaderBoardId : id of the leaderboard for which details are needed
     * @param profileId : user profile id, for which leaderboard is needed
     **/
    fun getLeaderBoardEntryForProfile(
        leaderBoardId: String,
        profileId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    )

    /**
     * Get leaderboard entries of current user profile with leaderboard id
     *
     ** @param leaderBoardId : id of the leaderboard
     **/
    fun getLeaderBoardEntryForCurrentUserProfile(
        leaderBoardId: String,
        liveLikeCallback: LiveLikeCallback<LeaderBoardEntry>
    )

    /**
     * Fetch leaderboard clients
     ** @param leaderBoardId : id of the leaderboard
     **/
    fun getLeaderboardClients(
        leaderBoardId: List<String>,
        liveLikeCallback: LiveLikeCallback<LeaderboardClient>
    )

}