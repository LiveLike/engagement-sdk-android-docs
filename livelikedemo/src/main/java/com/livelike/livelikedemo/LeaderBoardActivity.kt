package com.livelike.livelikedemo

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LeaderBoard
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntry
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.android.synthetic.main.activity_leader_board.btn_current_entry
import kotlinx.android.synthetic.main.activity_leader_board.btn_fetch
import kotlinx.android.synthetic.main.activity_leader_board.btn_next
import kotlinx.android.synthetic.main.activity_leader_board.btn_sort_down
import kotlinx.android.synthetic.main.activity_leader_board.btn_sort_up
import kotlinx.android.synthetic.main.activity_leader_board.ed_txt_program_id
import kotlinx.android.synthetic.main.activity_leader_board.prg_fetch_leader_boards
import kotlinx.android.synthetic.main.activity_leader_board.prg_leaderboard_entries
import kotlinx.android.synthetic.main.activity_leader_board.rcyl_leader_board
import kotlinx.android.synthetic.main.activity_leader_board.rcyl_leader_board_entries
import kotlinx.android.synthetic.main.lay_leader_board_list_item.view.lay_leader_board_item
import kotlinx.android.synthetic.main.lay_leader_board_list_item.view.textView5
import kotlinx.android.synthetic.main.lay_leader_board_list_item.view.txt_leaderboard_name
import kotlinx.android.synthetic.main.lay_leader_board_list_item.view.txt_reward_item_name

class LeaderBoardActivity : AppCompatActivity() {
    val adapter =
        LeaderBoardEntriesAdapter(object : RecyclerViewItemClickListener<LeaderBoardEntry> {
            override fun itemClick(item: LeaderBoardEntry) {
                leaderBoardId?.let {
                    dialog?.show()
                    (application as LiveLikeApplication).sdk.getLeaderBoardEntryForProfile(
                        leaderBoardId!!,
                        item.profile_id,
                        object : LiveLikeCallback<LeaderBoardEntry>() {
                            override fun onResponse(result: LeaderBoardEntry?, error: String?) {
                                dialog?.dismiss()
                                result?.let {
                                    showData(it)
                                }
                                error?.let {
                                    showToast(error)
                                }
                            }
                        })
                }
            }
        })
    var dialog: ProgressDialog? = null

    private fun showData(result: LeaderBoardEntry) {
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        AlertDialog.Builder(this@LeaderBoardActivity).apply {
            setTitle("Profile")
            setItems(
                arrayOf(
                    "NickName: ${result.profile_nickname}",
                    "Id: ${result.profile_id}",
                    "Percentile Rank: ${result.percentile_rank}",
                    "Rank: ${result.rank}",
                    "Score: ${result.score}"
                )
            ) { _, which ->
            }
            create()
        }.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_board)
        dialog = ProgressDialog.show(
            this, "",
            "Loading. Please wait...", true
        )
        rcyl_leader_board.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rcyl_leader_board_entries.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
//        ed_txt_program_id.setText("47c14e1d-5786-401e-a850-22c5a91a5399") //QA
//        ed_txt_program_id.setText("6834f1fd-f24d-4538-ba51-63544f9d78eb")//Prod
        rcyl_leader_board_entries.adapter = adapter
        btn_fetch.setOnClickListener {
            val programId = ed_txt_program_id.text.toString()
            if (programId.isNotEmpty()) {
                prg_fetch_leader_boards.visibility = View.VISIBLE
                (application as LiveLikeApplication).sdk.getLeaderBoardsForProgram(
                    programId, object : LiveLikeCallback<List<LeaderBoard>>() {
                        override fun onResponse(result: List<LeaderBoard>?, error: String?) {
                            prg_fetch_leader_boards.visibility = View.INVISIBLE
                            result?.let {
                                rcyl_leader_board.adapter = LeaderBoardAdapter(
                                    result,
                                    object : RecyclerViewItemClickListener<LeaderBoard> {
                                        override fun itemClick(item: LeaderBoard) {
                                            leaderBoardId = item.id
                                            loadEntries(LiveLikePagination.FIRST)
                                        }
                                    }
                                )
                            }
                            error?.let {
                                showToast(error)
                            }
                        }
                    }
                )
            }
        }
        btn_next.setOnClickListener {
            loadEntries(LiveLikePagination.NEXT)
        }
        btn_current_entry.setOnClickListener {
            leaderBoardId?.let { id ->
                dialog?.show()
                (application as LiveLikeApplication).sdk.getLeaderBoardEntryForCurrentUserProfile(id,
                    object : LiveLikeCallback<LeaderBoardEntry>() {
                        override fun onResponse(result: LeaderBoardEntry?, error: String?) {
                            dialog?.dismiss()
                            result?.let {
                                showData(it)
                            }
                            error?.let {
                                showToast(it)
                            }
                        }
                    })
            }
        }
        btn_sort_down.setOnClickListener {
            adapter.sortList(false)
        }
        btn_sort_up.setOnClickListener {
            adapter.sortList(true)
        }
        prg_leaderboard_entries.visibility = View.INVISIBLE
        dialog?.dismiss()
    }

    private var leaderBoardId: String? = null

    private fun loadEntries(pagination: LiveLikePagination) {
        leaderBoardId?.let {
            prg_leaderboard_entries.visibility = View.VISIBLE
            (application as LiveLikeApplication).sdk.getEntriesForLeaderBoard(
                leaderBoardId!!,
                pagination,
                object :
                    LiveLikeCallback<List<LeaderBoardEntry>>() {
                    override fun onResponse(
                        result: List<LeaderBoardEntry>?,
                        error: String?
                    ) {
                        prg_leaderboard_entries.visibility = View.INVISIBLE
                        if (pagination == LiveLikePagination.FIRST)
                            adapter.list.clear()
                        result?.let {
                            adapter.list.addAll(result)
                            adapter.notifyDataSetChanged()
                        }
                        error?.let {
                            showToast(error)
                        }
                    }
                })
        }
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

class LeaderBoardAdapter(
    private val list: List<LeaderBoard>,
    private val clickListener: RecyclerViewItemClickListener<LeaderBoard>
) :
    RecyclerView.Adapter<LeaderBoardViewHolder>() {

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
        p0.itemView.textView5.text = "Reward"
        p0.itemView.txt_reward_item_name.text = leaderBoard.rewardItem.name
        p0.itemView.lay_leader_board_item.setOnClickListener {
            clickListener.itemClick(leaderBoard)
        }
    }
}

interface RecyclerViewItemClickListener<T> {
    fun itemClick(item: T)
}

class LeaderBoardViewHolder(view: View) : RecyclerView.ViewHolder(view)

class LeaderBoardEntriesAdapter(private val recyclerViewItemClickListener: RecyclerViewItemClickListener<LeaderBoardEntry>) :
    RecyclerView.Adapter<LeaderBoardViewHolder>() {
    val list: ArrayList<LeaderBoardEntry> = arrayListOf()
    var isAscending = true
    fun sortList(isAsc: Boolean) {
        if (isAsc != isAscending) {
            isAscending = !isAscending
            list.reverse()
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): LeaderBoardViewHolder {
        return LeaderBoardViewHolder(
            LayoutInflater.from(p0.context)
                .inflate(R.layout.lay_leader_board_list_item, p0, false)
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(p0: LeaderBoardViewHolder, p1: Int) {
        val leaderBoard = list[p1]
        p0.itemView.txt_leaderboard_name.text = leaderBoard.profile_nickname
        p0.itemView.textView5.text = "Rank"
        p0.itemView.txt_reward_item_name.text = leaderBoard.rank.toString()
        p0.itemView.lay_leader_board_item.setOnClickListener {
            recyclerViewItemClickListener.itemClick(leaderBoard)
        }
    }
}
