package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.OptionsItem
import com.livelike.engagementsdk.core.data.models.NumberPredictionVotes
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.NumberPredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.widgetModel.NumberPredictionFollowUpWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.NumberPredictionWidgetModel
import com.livelike.livelikedemo.R
import com.livelike.livelikedemo.databinding.CustomNumberPredictionWidgetBinding
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.correct_op
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.img_1
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.minus
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.option_view_1
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.plus
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.text_1
import kotlinx.android.synthetic.main.custom_number_prediction_widget.view.claim_rewards
import kotlinx.android.synthetic.main.custom_number_prediction_widget.view.result_tv


class CustomNumberPredictionWidget :
    ConstraintLayout {
    var numberPredictionWidgetViewModel: NumberPredictionWidgetModel? = null
    var followUpWidgetViewModel: NumberPredictionFollowUpWidgetModel? = null
    private lateinit var binding: CustomNumberPredictionWidgetBinding
    var isImage = false
    var isFollowUp = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }


    private fun init() {
        binding = CustomNumberPredictionWidgetBinding.inflate(
            LayoutInflater.from(context),
            this@CustomNumberPredictionWidget,
            true
        )
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        var widgetData = numberPredictionWidgetViewModel?.widgetData
        if (isFollowUp) {
            widgetData = followUpWidgetViewModel?.widgetData
        }

        widgetData?.let { liveLikeWidget ->
            liveLikeWidget.options?.let { option ->
                if (option.size > 2) {
                    binding.rcylPredictionList.layoutManager =
                        GridLayoutManager(
                            context,
                            2
                        )
                }

                val adapter =
                    PredictionListAdapter(
                        context,
                        isImage,
                        ArrayList(option.map { item -> item!! })
                    )
                binding.rcylPredictionList.adapter = adapter
                binding.txt.text = liveLikeWidget.question

                getInteractedData(adapter) // get user interaction

                setOnClickListeners(adapter)
                if (isFollowUp) {
                    binding.btn1.visibility = View.GONE
                    claim_rewards.visibility = View.VISIBLE
                } else {
                    binding.btn1.visibility = View.VISIBLE
                    claim_rewards.visibility = View.GONE
                }

                if (isFollowUp) {
                    val votedList = followUpWidgetViewModel?.getPredictionVotes()
                    votedList?.forEach { op ->
                        adapter.predictionMap[op.optionId] = op.number
                    }
                    adapter.isFollowUp = true
                    verifyPredictedAnswer()
                } else {
                    result_tv.visibility = GONE
                }
            }

        }
    }

    private fun setOnClickListeners(adapter: PredictionListAdapter) {
        // predict button click
        binding.btn1.setOnClickListener {
            if (!isFollowUp) {
                val optionList = submitVoteRequest(adapter)
                numberPredictionWidgetViewModel?.lockInVote(optionList).apply {
                    Toast.makeText(context, "score submitted", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.imgClose.setOnClickListener {
            finish()
        }

        claim_rewards.setOnClickListener{
            followUpWidgetViewModel?.claimRewards()
        }
    }



    private fun submitVoteRequest(adapter: PredictionListAdapter):List<NumberPredictionVotes> {
        val optionList = mutableListOf<NumberPredictionVotes>()
        val maps = adapter.getPredictedScore()
        if(maps.isNullOrEmpty()){
            val options = numberPredictionWidgetViewModel?.widgetData?.options
            for(item in options!!){
                optionList.add(
                    NumberPredictionVotes(
                        optionId = item?.id!!,
                        number = 0
                    )
                )
            }
        }else{
            for (item in maps) {
                optionList.add(
                    NumberPredictionVotes(
                        optionId = item.key,
                        number = item.value
                    )
                )
            }
        }
        return optionList
    }


    /**
     * verify if votes submitted are correct
     */
    private fun verifyPredictedAnswer() {
        @Suppress("UnusedDeclaration") // TODO: [CAF] evaluate what this actually is doing?
        var isCorrect = false
        followUpWidgetViewModel?.widgetData?.options?.let { option ->
            val votedList = followUpWidgetViewModel?.getPredictionVotes()
            if (option.size == votedList?.size) {
                for (i in votedList.indices) {
                    val votedOption = votedList[i]

                    val op = option.find { it?.id == votedOption.optionId }
                    isCorrect = op != null && votedOption.number == op.correctNumber
                }
            }
        }
    }


    //get user interacted data from load history api
    private fun getInteractedData(adapter: PredictionListAdapter) {
        val lists = numberPredictionWidgetViewModel?.getUserInteraction()
        lists?.votes?.let { scores ->
            Log.d("CustomPredictionWidget", "CustomNoPredictionWidget.onResponse>>${scores}")
            adapter.setInteractedData(scores)
            adapter.notifyDataSetChanged()
        }

        // this is just to test the load interaction api
        numberPredictionWidgetViewModel?.loadInteractionHistory(object :
            LiveLikeCallback<List<NumberPredictionWidgetUserInteraction>>() {
            override fun onResponse(
                result: List<NumberPredictionWidgetUserInteraction>?,
                error: String?
            ) {
                if (!result.isNullOrEmpty()) {
                    Log.d(
                        "CustomPredictionWidget",
                        "CustomNoPredictionWidget.onResponse>>${result}"
                    )
                }
            }
        })

    }

    fun finish() {
        numberPredictionWidgetViewModel?.finish()
        followUpWidgetViewModel?.finish()
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // numberPredictionWidgetViewModel?.voteResults?.unsubscribe(this)
    }

    class PredictionListAdapter(
        private val context: Context,
        private val isImage: Boolean,
        val list: ArrayList<OptionsItem>
    ) : RecyclerView.Adapter<PredictionListAdapter.PredictionListItemViewHolder>() {

        var predictionMap: HashMap<String, Int> = HashMap()
        var isFollowUp = false

        fun getPredictedScore(): HashMap<String, Int> {
            return predictionMap
        }

        class PredictionListItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): PredictionListItemViewHolder {
            return PredictionListItemViewHolder(
                LayoutInflater.from(parent.context!!).inflate(
                    R.layout.custom_number_prediction_item,
                    parent, false
                )
            )
        }

        override fun onBindViewHolder(
            holder: PredictionListItemViewHolder,
            position: Int
        ) {
            val item = list[position]

            if (isImage) {
                Glide.with(context)
                    .load(item.imageUrl)
                    .into(
                        holder.itemView.img_1
                    )
                holder.itemView.text_1.visibility = View.GONE
                holder.itemView.img_1.visibility = View.VISIBLE

            } else {
                holder.itemView.text_1.text = item.description
                holder.itemView.text_1.visibility = View.VISIBLE
                holder.itemView.img_1.visibility = View.GONE
            }

            if (isFollowUp) {
                holder.itemView.option_view_1.text = "${predictionMap[item.id!!] ?: 0}"
                holder.itemView.correct_op.text =  item.correctNumber.toString()
                holder.itemView.correct_op.visibility = View.VISIBLE
            }else{
                holder.itemView.correct_op.visibility = View.GONE
            }

            if (!isFollowUp) {
                if (item.number != null) {
                    holder.itemView.option_view_1.text = item.number.toString()
                } else {
                    holder.itemView.option_view_1.text = "0"
                }
            }


            holder.itemView.plus.setOnClickListener {
                if (!isFollowUp) {
                    val updatedScore = holder.itemView.option_view_1.text.toString().toInt() + 1
                    holder.itemView.option_view_1.text = updatedScore.toString()
                    predictionMap[item.id!!] = updatedScore
                }

            }

            holder.itemView.minus.setOnClickListener {
                if (!isFollowUp) {
                    val updatedScore = holder.itemView.option_view_1.text.toString().toInt() - 1
                    holder.itemView.option_view_1.text = updatedScore.toString()
                    predictionMap[item.id!!] = updatedScore
                }
            }
        }


        override fun getItemCount(): Int = list.size

        fun setInteractedData(interactedList: List<NumberPredictionVotes>) {
            for (i in list.indices) {
                if (list[i].id == interactedList[i].optionId) list[i].number =
                    interactedList[i].number
            }
        }
    }
}