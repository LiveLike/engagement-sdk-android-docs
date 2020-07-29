package com.livelike.livelikedemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.core.data.models.LeaderBoard
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.android.synthetic.main.activity_leader_board.btn_fetch
import kotlinx.android.synthetic.main.activity_leader_board.ed_txt_program_id
import kotlinx.android.synthetic.main.activity_leader_board.prg_fetch_leader_boards
import kotlinx.android.synthetic.main.activity_leader_board.rcyl_leader_board
import kotlinx.android.synthetic.main.lay_leader_board_list_item.view.txt_leaderboard_name
import kotlinx.android.synthetic.main.lay_leader_board_list_item.view.txt_reward_item_name

class LeaderBoardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_board)
        rcyl_leader_board.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        ed_txt_program_id.setText("47c14e1d-5786-401e-a850-22c5a91a5399")
        btn_fetch.setOnClickListener {
            val programId = ed_txt_program_id.text.toString()
            if (programId.isNotEmpty()) {
                prg_fetch_leader_boards.visibility = View.VISIBLE
                (application as LiveLikeApplication).sdk.getLeaderBoardsForProgram(
                    programId, object : LiveLikeCallback<List<LeaderBoard>>() {
                        override fun onResponse(result: List<LeaderBoard>?, error: String?) {
                            prg_fetch_leader_boards.visibility = View.INVISIBLE
                            result?.let {
                                rcyl_leader_board.adapter = LeaderBoardAdapter(result)
                            }
                        }
                    }
                )
            }
        }
    }
}

class LeaderBoardAdapter(private val list: List<LeaderBoard>) :
    RecyclerView.Adapter<LeaderBoardAdapter.LeaderBoardViewHolder>() {

    class LeaderBoardViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): LeaderBoardViewHolder {
        return LeaderBoardViewHolder(
            LayoutInflater.from(p0.context)
                .inflate(R.layout.lay_leader_board_list_item, p0, false)
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(p0: LeaderBoardViewHolder, p1: Int) {
        val leaderBoard = list[p1]
        p0.itemView.txt_leaderboard_name.text = leaderBoard.name
        p0.itemView.txt_reward_item_name.text = leaderBoard.rewardItem.name
    }
}