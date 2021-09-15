package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.util.AttributeSet
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
import com.livelike.engagementsdk.widget.widgetModel.NumberPredictionFollowUpWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.NumberPredictionWidgetModel
import com.livelike.livelikedemo.R
import com.livelike.livelikedemo.databinding.CustomNumberPredictionWidgetBinding
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.img_1
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.minus
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.option_view_1
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.plus
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.text_1



class CustomNumberPredictionWidget :
    ConstraintLayout {
    var numberPredictionWidgetViewModel: NumberPredictionWidgetModel? = null
    var followUpWidgetViewModel: NumberPredictionFollowUpWidgetModel? = null
    private lateinit var binding: CustomNumberPredictionWidgetBinding
    var isImage = false
    var isFollowUp = false

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }


    private fun init(attrs: AttributeSet?, defStyle: Int) {
        binding = CustomNumberPredictionWidgetBinding.inflate(
            LayoutInflater.from(context),
            this@CustomNumberPredictionWidget,
            true
        )
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        var widgetData = numberPredictionWidgetViewModel?.widgetData
        // var voteResults = numberPredictionWidgetViewModel?.voteResults
        if (isFollowUp) {
            widgetData = followUpWidgetViewModel?.widgetData
            //voteResults = followUpWidgetViewModel?.voteResults
        }
        widgetData?.let { liveLikeWidget ->

            /*    //load interaction history
            numberPredictionWidgetViewModel?.loadInteractionHistory(object : LiveLikeCallback<List<NumberPredictionWidgetUserInteraction>>() {
                override fun onResponse(
                    result: List<NumberPredictionWidgetUserInteraction>?,
                    error: String?
                ) {
                    if (result != null) {
                        if (result.isNotEmpty()) {
                            for (element in result) {
                                Log.d("interaction-prediction", element.optionId)
                            }
                        }
                    }
                }
            })*/

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
                enableLockButton()

                // predict button click
                binding.btn1.setOnClickListener {
                    if (!isFollowUp) {
                        val map = adapter.getPredictedScore()
                        val optionList = mutableListOf<NumberPredictionVotes>()
                        map.forEach { item ->
                            optionList.add(
                                NumberPredictionVotes(
                                    optionId = item.key,
                                    number = item.value
                                )
                            )
                        }
                        numberPredictionWidgetViewModel?.lockInVote(optionList).apply {
                            disableLockButton()
                            Toast.makeText(context, "score submitted", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                binding.txt.text = liveLikeWidget.question

                binding.imgClose.setOnClickListener {
                    finish()
                    this.removeAllViews()
                }

                if (isFollowUp) {
                    binding.btn1.visibility = View.GONE
                }else{
                    binding.btn1.visibility = View.VISIBLE
                }

                var isCorrect = false
                if (isFollowUp) {
                    val votedList = followUpWidgetViewModel?.getPredictionVotes()
                            if(option.size == votedList?.size){
                                for (i in option.indices) {
                                    isCorrect = option[i]?.id == votedList[i].optionId && option[i]?.correctNumber == votedList[i].number
                                }
                                    Toast.makeText(
                                        context,
                                        when (isCorrect) {
                                            true -> "Correct"
                                            else -> "Incorrect"
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                            }
                }

                if (isFollowUp) {
                    option.forEach { op ->
                        adapter.predictionMap[op?.id!!] = op.correctNumber ?: 0
                    }
                    adapter.isFollowUp = true
                }

            }
        }
    }

    fun finish() {
        numberPredictionWidgetViewModel?.finish()
        followUpWidgetViewModel?.finish()
    }

    private fun enableLockButton() {
        binding.btn1.isEnabled = true
        binding.btn1.alpha = 1f
    }

    private fun disableLockButton() {
        binding.btn1.isEnabled = false
        binding.btn1.alpha = 0.5f
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

    }
}