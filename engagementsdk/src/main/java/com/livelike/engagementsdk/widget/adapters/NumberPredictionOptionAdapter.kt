package com.livelike.engagementsdk.widget.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.FontFamilyProvider
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.widget.OptionsWidgetThemeComponent
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Option
import kotlinx.android.synthetic.main.livelike_increment_decrement.view.minus_btn
import kotlinx.android.synthetic.main.livelike_increment_decrement.view.plus_btn
import kotlinx.android.synthetic.main.livelike_increment_decrement.view.userInput
import kotlinx.android.synthetic.main.widget_number_prediction_item.view.description
import kotlinx.android.synthetic.main.widget_number_prediction_item.view.imgView


internal class NumberPredictionOptionAdapter(
    private var myDataset: List<Option>,
    private val widgetType: WidgetType,
    var component: OptionsWidgetThemeComponent? = null
) : RecyclerView.Adapter<NumberPredictionOptionAdapter.OptionViewHolder>(){

    var fontFamilyProvider: FontFamilyProvider? = null
    var selectionLocked = false


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        return OptionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.widget_number_prediction_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        val item = myDataset[position]
        val itemIsLast = myDataset.size - 1 == position

        // sets the data
        holder.setData(
            item,
            widgetType,
            itemIsLast,
            component,
            fontFamilyProvider
        )
    }

    override fun getItemCount() = myDataset.size


    inner class OptionViewHolder(view: View):RecyclerView.ViewHolder(view){

        fun setData(
            option: Option,
            widgetType: WidgetType,
            itemIsLast: Boolean,
            component: OptionsWidgetThemeComponent?,
            fontFamilyProvider: FontFamilyProvider?
        ){
            if (!option.image_url.isNullOrEmpty()){
                Glide.with(itemView.context.applicationContext)
                    .load(option.image_url)
                    .into(itemView.imgView)
            }

            itemView.description.text = option.description

            itemView.plus_btn.setOnClickListener {
                val updatedScore: Int? = if(itemView.userInput.text.isNullOrEmpty()){
                    0
                }else{
                    itemView.userInput.text.toString().toInt() + 1
                }
                itemView.userInput.setText(updatedScore.toString())
            }

            itemView.minus_btn.setOnClickListener {
                val updatedScore: Int? = if(itemView.userInput.text.isNullOrEmpty()){
                    0
                }else{
                    itemView.userInput.text.toString().toInt() - 1
                }
                itemView.userInput.setText(updatedScore.toString())
            }

        }
    }
}