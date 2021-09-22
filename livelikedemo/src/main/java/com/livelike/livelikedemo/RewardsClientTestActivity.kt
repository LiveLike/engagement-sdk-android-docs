package com.livelike.livelikedemo

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LLPaginatedResult
import com.livelike.engagementsdk.core.data.models.RewardItem
import com.livelike.engagementsdk.gamification.IRewardsClient
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.android.synthetic.main.reward_is_client_test_activity.progress_bar
import kotlinx.android.synthetic.main.reward_is_client_test_activity.reward_item_balance
import kotlinx.android.synthetic.main.reward_is_client_test_activity.reward_item_spinnner
import kotlinx.android.synthetic.main.reward_is_client_test_activity.send_btn

class RewardsClientTestActivity : AppCompatActivity() {

    private lateinit var rewardsClient: IRewardsClient
    var rewardIems: List<RewardItem>? = null

    var selectedrewardItem: RewardItem? = null

    var rewardItemBalanceMap: Map<String, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reward_is_client_test_activity)

        rewardsClient = (applicationContext as LiveLikeApplication).sdk.rewards()

        rewardsClient.getApplicationRewardItems(
            LiveLikePagination.FIRST,
            object : LiveLikeCallback<LLPaginatedResult<RewardItem>>() {
                override fun onResponse(result: LLPaginatedResult<RewardItem>?, error: String?) {
                    result?.results?.let {
                        fetchRewardItemBalances(it.map { it.id })
                        rewardIems = it
                    }
                    showError(error)
                }
            })

    }

    fun fetchRewardItemBalances(ids: List<String>) {
        rewardsClient.getRewardItemsBalance(
            ids,
            object : LiveLikeCallback<Map<String, Int>>() {
                override fun onResponse(result: Map<String, Int>?, error: String?) {
                    result?.let {
                        rewardItemBalanceMap = result
                        runOnUiThread {
                            initUI()
                        }
                    }
                    showError(error)
                }
            })
    }

    private fun initUI() {
        progress_bar.visibility = View.GONE

        rewardIems?.let { rewardIems->

            val adapter = ArrayAdapter(this,android.R.layout.simple_spinner_item,rewardIems.map { it.name })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            reward_item_spinnner.adapter = adapter

            reward_item_spinnner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedrewardItem = rewardIems[position]
                        reward_item_balance.text =
                            "Balance : ${(rewardItemBalanceMap?.get(selectedrewardItem?.id ?: "") ?: "0")}"
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }

                }

            send_btn.setOnClickListener {
                if(selectedrewardItem==null){
                    showError("Please select reward item")
                }else{

                }
            }
        }

    }

    fun showError(error: String?) {
        error?.let {
            runOnUiThread {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }


}