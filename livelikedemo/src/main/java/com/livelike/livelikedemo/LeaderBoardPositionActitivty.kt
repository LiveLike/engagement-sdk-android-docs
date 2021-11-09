package com.livelike.livelikedemo

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.core.data.models.LeaderBoard
import com.livelike.engagementsdk.core.data.models.LeaderBoardForClient
import com.livelike.engagementsdk.core.data.models.LeaderboardClient
import com.livelike.engagementsdk.core.data.models.LeaderboardPlacement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.domain.LeaderBoardDelegate
import com.livelike.livelikedemo.channel.ChannelManager
import kotlinx.android.synthetic.main.activity_leader_board_position_actitivty.leaderBoardLayout

class LeaderBoardPositionActitivty : AppCompatActivity() {

    private lateinit var session: LiveLikeContentSession
    private lateinit var channelManager: ChannelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_board_position_actitivty)

        channelManager = (application as LiveLikeApplication).channelManager
        session = (application as LiveLikeApplication).createPublicSession(
            channelManager.selectedChannel.llProgram.toString(), allowTimeCodeGetter = false
        )

        (application as LiveLikeApplication).sdk.getLeaderBoardsForProgram(
            channelManager.selectedChannel.llProgram.toString(),
            object : LiveLikeCallback<List<LeaderBoard>>() {
                override fun onResponse(result: List<LeaderBoard>?, error: String?) {
                    result?.let {

                        val listOfLeaderBoardIds: ArrayList<String> = ArrayList()
                        it.map {
                            listOfLeaderBoardIds.add(it.id)
                        }
                        (application as LiveLikeApplication).sdk.getLeaderboardClients(
                            listOfLeaderBoardIds,
                            object : LiveLikeCallback<LeaderboardClient>() {
                                override fun onResponse(
                                    result: LeaderboardClient?,
                                    error: String?
                                ) {

                                    result?.let {
                                        result
                                    }
                                    error?.let {
                                    }
                                }
                            }
                        )
                        (applicationContext as LiveLikeApplication).sdk.leaderBoardDelegate =
                            object :
                                LeaderBoardDelegate {
                                override fun leaderBoard(
                                    leaderBoard: LeaderBoardForClient,
                                    currentUserPlacementDidChange: LeaderboardPlacement
                                ) {
                                    runOnUiThread {
                                        // Stuff that updates the UI
                                        val textViewRow = TextView(this@LeaderBoardPositionActitivty)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            textViewRow.setTextColor(resources.getColor(R.color.colorAccent, theme))
                                        } else {
                                            @Suppress("DEPRECATION") //kept due to pre M support
                                            textViewRow.setTextColor(resources.getColor(R.color.colorAccent))
                                        }
                                        textViewRow.text = "\"LeaderBoardName\" + ${leaderBoard.name} + \"Rank: \"+ ${currentUserPlacementDidChange.rank}+ \"Percentile\"+ ${currentUserPlacementDidChange.rankPercentile} + \"Score \"+ ${currentUserPlacementDidChange.score}"
                                        leaderBoardLayout.addView(textViewRow)
                                    }
                                }
                            }
                    }
                    error?.let {
                    }
                }
            }
        )
    }
}
