package com.livelike.livelikedemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.livelike.engagementsdk.core.data.models.LeaderBoard
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.android.synthetic.main.activity_leader_board.btn_fetch
import kotlinx.android.synthetic.main.activity_leader_board.ed_txt_program_id

class LeaderBoardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_board)
        btn_fetch.setOnClickListener {
            val programId = ed_txt_program_id.text.toString()
            if (programId.isNotEmpty()) {
                (application as LiveLikeApplication).sdk.getLeaderBoardsForProgram(
                    programId, object : LiveLikeCallback<List<LeaderBoard>>() {
                        override fun onResponse(result: List<LeaderBoard>?, error: String?) {
                            
                        }
                    }
                )
            }
        }
    }
}