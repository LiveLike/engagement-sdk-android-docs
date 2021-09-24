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
import com.livelike.engagementsdk.core.utils.validateUuid
import com.livelike.engagementsdk.gamification.IRewardsClient
import com.livelike.engagementsdk.gamification.TransferRewardItemResponse
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.android.synthetic.main.reward_is_client_test_activity.enter_amount_et
import kotlinx.android.synthetic.main.reward_is_client_test_activity.progress_bar
import kotlinx.android.synthetic.main.reward_is_client_test_activity.receipent_profile_id
import kotlinx.android.synthetic.main.reward_is_client_test_activity.reward_item_balance
import kotlinx.android.synthetic.main.reward_is_client_test_activity.reward_item_spinnner
import kotlinx.android.synthetic.main.reward_is_client_test_activity.send_btn

class RewardsClientTestActivity : AppCompatActivity() {

    private lateinit var rewardsClient: IRewardsClient
    var rewardIems: List<RewardItem>? = null

    var selectedrewardItem: RewardItem? = null

    val rewardItemBalanceMap: MutableMap<String, Int> = mutableMapOf()

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
                        rewardItemBalanceMap.putAll(result)
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

        rewardIems?.let { rewardIems ->

            val adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_item, rewardIems.map { it.name })
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
                if (selectedrewardItem == null) {
                    showError("Please select reward item")
                } else {
                    if (!validateUuid(receipent_profile_id.text.toString())) {
                        showError("Please Enter valid recipeint id")
                        return@setOnClickListener
                    }
                    if (enter_amount_et.text.isEmpty()) {
                        showError("Please Enter amount")
                        return@setOnClickListener
                    }
                    if (enter_amount_et.text.isBlank() || enter_amount_et.text.toString().toInt() > (rewardItemBalanceMap?.get(
                            selectedrewardItem?.id ?: ""
                        ) ?: 0)
                    ) {
                        showError("Please Enter amount less than or equal to available balance")
                        return@setOnClickListener
                    }

                    progress_bar.visibility = View.VISIBLE

                    rewardsClient.transferAmountToProfileId(selectedrewardItem!!.id,
                        enter_amount_et.text.toString().toInt(),
                        receipent_profile_id.text.toString(),
                        object : LiveLikeCallback<TransferRewardItemResponse>() {
                            override fun onResponse(
                                result: TransferRewardItemResponse?,
                                error: String?
                            ) {
                                result?.let {
                                    rewardItemBalanceMap.put(
                                        selectedrewardItem?.id ?: "",
                                        it.newBalance
                                    )
                                }
                                runOnUiThread {
                                    enter_amount_et.setText("")
                                    reward_item_balance.text =
                                        "Balance : ${(rewardItemBalanceMap?.get(selectedrewardItem?.id ?: "") ?: "0")}"
                                    progress_bar.visibility = View.GONE
                                    showError("amount sent successfully")
                                }
                                showError(error)
                            }

                        })

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