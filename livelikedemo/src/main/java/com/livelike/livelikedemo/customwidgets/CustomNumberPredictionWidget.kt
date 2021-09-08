package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.OptionsItem
import com.livelike.engagementsdk.widget.viewModel.CheerMeterVoteState
import com.livelike.engagementsdk.widget.widgetModel.NumberPredictionWidgetModel
import com.livelike.livelikedemo.R
import com.livelike.livelikedemo.databinding.CustomNumberPredictionWidgetBinding
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.img_1
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.minus
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.option_view_1
import kotlinx.android.synthetic.main.custom_number_prediction_item.view.plus


class CustomNumberPredictionWidget :
    ConstraintLayout {
    var numberPredictionWidgetViewModel: NumberPredictionWidgetModel? = null
    private lateinit var binding: CustomNumberPredictionWidgetBinding

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
                    PredictionListAdapter(context, ArrayList(option.map { item -> item!! }))
                binding.rcylPredictionList.adapter = adapter

                // predict button click
                binding.btn1.setOnClickListener {
                    val map = adapter.getPredictedScore()
                    var optionList = mutableListOf<OptionsItem>()
                    map.forEach { map ->

                        var optionItem = option.find {
                            it?.id == map.key
                        }
                        // change/add value in the option item and then add to the array list
                        if (optionItem != null) {
                            optionList.add(optionItem)
                        }
                    }
                }

            }

            binding.txt.text = liveLikeWidget.question

            binding.imgClose.setOnClickListener {
                numberPredictionWidgetViewModel?.finish()
                this.removeAllViews()
            }

        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    class PredictionListAdapter(
        private val context: Context,
        val list: ArrayList<OptionsItem>
    ) : RecyclerView.Adapter<PredictionListAdapter.PredictionListItemViewHolder>() {

        var predictionMap: HashMap<String, Int> = HashMap()

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

            /* Glide.with(context)
                 .load(item.imageUrl)
                 .into(holder.itemView.img_1
                 )*/

            holder.itemView.img_1.text = item.description

            holder.itemView.plus.setOnClickListener {
                var updatedScore = holder.itemView.option_view_1.text.toString().toInt() + 1
                holder.itemView.option_view_1.text = updatedScore.toString()
                predictionMap[item.id!!] = updatedScore
            }

            holder.itemView.minus.setOnClickListener {
                var updatedScore = holder.itemView.option_view_1.text.toString().toInt() - 1
                holder.itemView.option_view_1.text = updatedScore.toString()
                predictionMap[item.id!!] = updatedScore
            }
        }


        override fun getItemCount(): Int = list.size

    }
}