package com.livelike.livelikedemo

import android.app.AlertDialog
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
import com.livelike.engagementsdk.gamification.RewardEventsListener
import com.livelike.engagementsdk.gamification.RewardItemBalance
import com.livelike.engagementsdk.gamification.RewardItemTransferRequestParams
import com.livelike.engagementsdk.gamification.RewardItemTransferType
import com.livelike.engagementsdk.gamification.RewardTransaction
import com.livelike.engagementsdk.gamification.RewardTransactionsRequestParameters
import com.livelike.engagementsdk.gamification.TransferRewardItem
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.WidgetType
import kotlinx.android.synthetic.main.reward_is_client_test_activity.button_get_reward
import kotlinx.android.synthetic.main.reward_is_client_test_activity.enter_amount_et
import kotlinx.android.synthetic.main.reward_is_client_test_activity.filter_by_widget_kind
import kotlinx.android.synthetic.main.reward_is_client_test_activity.progress_bar
import kotlinx.android.synthetic.main.reward_is_client_test_activity.receipent_profile_id
import kotlinx.android.synthetic.main.reward_is_client_test_activity.reward_item_balance
import kotlinx.android.synthetic.main.reward_is_client_test_activity.reward_item_spinnner
import kotlinx.android.synthetic.main.reward_is_client_test_activity.reward_uuid_text
import kotlinx.android.synthetic.main.reward_is_client_test_activity.send_btn
import kotlinx.android.synthetic.main.reward_is_client_test_activity.show_all_reward_transfers
import kotlinx.android.synthetic.main.reward_is_client_test_activity.transfer_type_selection

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
                    showToast(error)
                }
            })

        rewardsClient.rewardEventsListener = object : RewardEventsListener() {
            override fun onReceiveNewRewardItemTransfer(rewardItemTransfer: TransferRewardItem) {
                runOnUiThread {  showAllRewardsTransfers() }
            }
        }

    }

    fun fetchRewardItemBalances(ids: List<String>) {
        rewardsClient.getRewardItemBalances(
            LiveLikePagination.FIRST,
            ids,
            object : LiveLikeCallback<LLPaginatedResult<RewardItemBalance>>() {
                override fun onResponse(result: LLPaginatedResult<RewardItemBalance>?, error: String?) {
                    result?.let {
                        result.results?.forEach {
                            rewardItemBalanceMap.put(it.rewardItemId,it.rewardItemBalance)
                        }
                        runOnUiThread {
                            initUI()
                        }
                    }
                    showToast(error)
                }
            })
    }

    private fun initUI() {
        progress_bar.visibility = View.GONE
        receipent_profile_id.setText("26722d0d-c6db-417f-8395-eacb1afb019f")

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
                            "Balance : ${(rewardItemBalanceMap.get(selectedrewardItem?.id ?: "") ?: "0")}"
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }

                }

            send_btn.setOnClickListener {
                if (selectedrewardItem == null) {
                    showToast("Please select reward item")
                } else {
                    if (!validateUuid(receipent_profile_id.text.toString())) {
                        showToast("Please Enter valid recipeint id")
                        return@setOnClickListener
                    }
                    if (enter_amount_et.text.isEmpty()) {
                        showToast("Please Enter amount")
                        return@setOnClickListener
                    }
                    if (enter_amount_et.text.isBlank() || enter_amount_et.text.toString()
                            .toInt() > (rewardItemBalanceMap.get(
                            selectedrewardItem?.id ?: ""
                        ) ?: 0)
                    ) {
                        showToast("Please Enter amount less than or equal to available balance")
                        return@setOnClickListener
                    }

                    progress_bar.visibility = View.VISIBLE

                    rewardsClient.transferAmountToProfileId(selectedrewardItem!!.id,
                        enter_amount_et.text.toString().toInt(),
                        receipent_profile_id.text.toString(),
                        object : LiveLikeCallback<TransferRewardItem>() {
                            override fun onResponse(
                                result: TransferRewardItem?,
                                error: String?
                            ) {
                                runOnUiThread {
                                    enter_amount_et.setText("")
                                    reward_item_balance.text =
                                        "Balance : ${(rewardItemBalanceMap.get(selectedrewardItem?.id ?: "") ?: "0")}"
                                    progress_bar.visibility = View.GONE
                                }
                                if (error == null) {
                                    showToast("amount sent successfully")
                                } else {
                                    showToast(error)
                                }
                            }

                        })

                }
            }
        }

        show_all_reward_transfers.setOnClickListener {
            showAllRewardsTransfers()
        }

        filter_by_widget_kind.setOnClickListener {

            val checked = mutableSetOf<WidgetType>()

            AlertDialog.Builder(this)
                .setTitle("choose kinds")
                .setMultiChoiceItems(
                    WidgetType.values().map {
                        it.event
                            .replace("-created", "")
                            .replace("-updated", "")
                    }.toTypedArray(),
                    BooleanArray(WidgetType.values().size)
                ) { _, which, isChecked ->
                    if (isChecked) {
                        checked.add(WidgetType.values()[which])
                    } else {
                        checked.remove(WidgetType.values()[which])
                    }
                }
                .setPositiveButton("ok") { _,_ ->
                    rewardsClient.getRewardTransactions(
                        LiveLikePagination.FIRST,
                        RewardTransactionsRequestParameters( widgetKindFilter = checked),
                        object : LiveLikeCallback<LLPaginatedResult<RewardTransaction>>() {
                            override fun onResponse(
                                result: LLPaginatedResult<RewardTransaction>?,
                                error: String?
                            ) {
                                runOnUiThread{ buildResultDialog( result, error) }
                            }
                        }
                    )
                }
                .create()
                .show()
        }

        button_get_reward.setOnClickListener {

            if( !reward_uuid_text.text.isNullOrBlank() && !validateUuid(reward_uuid_text.text.toString())){
                buildResultDialog( null, "enter a valid uuid")
                return@setOnClickListener
            }

            rewardsClient.getRewardTransactions(
                LiveLikePagination.FIRST,
                RewardTransactionsRequestParameters( widgetIds = setOf(reward_uuid_text.text.toString()) ),
                object : LiveLikeCallback<LLPaginatedResult<RewardTransaction>>() {
                    override fun onResponse(
                        result: LLPaginatedResult<RewardTransaction>?,
                        error: String?
                    ) {
                        runOnUiThread{ buildResultDialog( result, error) }
                    }
                }
            )
        }
    }

    private fun buildResultDialog(result: LLPaginatedResult<RewardTransaction>?, error: String?) {
        error?.let {
            AlertDialog.Builder( this)
                .setMessage(it)
                .create()
                .show()
        }

        result?.let{
            AlertDialog.Builder ( this )
                .setItems( it.results?.map {
                    "reward UUID: ${it.id}, Kind: ${it.widgetKind}"
                }?.toTypedArray() ){ _, _ -> }
                .create()
                .show()
        }
    }

    private fun showAllRewardsTransfers() {
        progress_bar.visibility = View.VISIBLE
        if(transfer_type_selection.checkedRadioButtonId==-1){
            rewardsClient.getRewardItemTransfers(
                LiveLikePagination.FIRST,
                object : LiveLikeCallback<LLPaginatedResult<TransferRewardItem>>() {
                    override fun onResponse(
                        result: LLPaginatedResult<TransferRewardItem>?,
                        error: String?
                    ) {
                        onRewardTransferListResponse(result, error)
                    }
                })
        }else {
            val requestParams = if (transfer_type_selection.checkedRadioButtonId == R.id.sent) {
                RewardItemTransferRequestParams(RewardItemTransferType.SENT)
            } else {
                RewardItemTransferRequestParams(RewardItemTransferType.RECEIVED)
            }
            rewardsClient.getRewardItemTransfers(
                LiveLikePagination.FIRST,
                requestParams,
                object : LiveLikeCallback<LLPaginatedResult<TransferRewardItem>>() {
                    override fun onResponse(
                        result: LLPaginatedResult<TransferRewardItem>?,
                        error: String?
                    ) {
                        onRewardTransferListResponse(result, error)
                    }
                })
        }
    }

    private fun onRewardTransferListResponse(
        result: LLPaginatedResult<TransferRewardItem>?,
        error: String?
    ) {
        runOnUiThread {
            progress_bar.visibility = View.GONE
            result?.results?.let {
                val list = it.map { getRewardTransferString(it) }
                AlertDialog.Builder(this@RewardsClientTestActivity).apply {
                    setTitle("Rewards transfer list")
                    setItems(list.toTypedArray()) { _, _ ->
                    }
                    create()
                }.show()

            }
        }
        showToast(error)
    }

    private fun getRewardTransferString(transferRewardItem: TransferRewardItem): String {
        val transferRewardItemRow = StringBuilder()
        val rewardItem = rewardIems?.find { transferRewardItem.rewardItemId == it.id }?.name ?: "NA"
        if (transferRewardItem.senderProfileId == ((applicationContext as LiveLikeApplication).sdk.userStream.latest()?.userId
                ?: "")
        ) {
            transferRewardItemRow.append("Sent ${transferRewardItem.rewardItemAmount} $rewardItem to ${transferRewardItem.recipientProfileId}")
        } else {
            transferRewardItemRow.append("Received ${transferRewardItem.rewardItemAmount} $rewardItem from ${transferRewardItem.senderProfileId}")
        }
        return transferRewardItemRow.toString()
    }

    fun showToast(message: String?) {
        message?.let {
            runOnUiThread {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }


}