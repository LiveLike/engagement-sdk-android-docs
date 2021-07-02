package com.livelike.livelikedemo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.PredictionWidgetUserInteraction
import com.livelike.livelikedemo.adapters.UnclaimedInteractionAdapter
import kotlinx.android.synthetic.main.activity_sponsor_test.progress_bar
import kotlinx.android.synthetic.main.activity_unclaimed_interaction.claim_rv
import kotlinx.android.synthetic.main.activity_unclaimed_interaction.unclaimed_btn


class UnclaimedInteractionActivity : AppCompatActivity() {

    private var session: LiveLikeContentSession? = null
    var adapter: UnclaimedInteractionAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unclaimed_interaction)
        session =
            (application as LiveLikeApplication).createPublicSession((application as LiveLikeApplication)?.channelManager.selectedChannel.llProgram.toString())

        unclaimed_btn.setOnClickListener { fetchUnclaimedInteractions() }

    }


    private fun fetchUnclaimedInteractions() {
        var unclaimedWidgetList: MutableList<LiveLikeWidget>? = ArrayList<LiveLikeWidget>()
        progress_bar.visibility = View.VISIBLE
        session?.getWidgetInteractionsWithUnclaimedRewards(
            LiveLikePagination.FIRST,
            object : LiveLikeCallback<List<PredictionWidgetUserInteraction>>() {
                override fun onResponse(
                    result: List<PredictionWidgetUserInteraction>?,
                    error: String?
                ) {
                    progress_bar.visibility = View.GONE
                    result?.let {
                        Log.d("unclaimed", "interaction list loaded-> ${result.size}")
                        // set adapter
                        unclaimedWidgetList?.let {
                            adapter = UnclaimedInteractionAdapter(
                                this@UnclaimedInteractionActivity,
                                (application as LiveLikeApplication).sdk,session, result
                            )
                            claim_rv.layoutManager =
                                LinearLayoutManager(this@UnclaimedInteractionActivity)
                            claim_rv.adapter = adapter
                        }
                    }
                }
            })
    }
}