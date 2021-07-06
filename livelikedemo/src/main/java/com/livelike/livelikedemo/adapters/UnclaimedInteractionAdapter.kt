package com.livelike.livelikedemo.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.data.models.PredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.view.WidgetView
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import com.livelike.engagementsdk.widget.widgetModel.FollowUpWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.ImageSliderWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PredictionWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel
import com.livelike.livelikedemo.R
import com.livelike.livelikedemo.customwidgets.CustomPredictionWidget

class UnclaimedInteractionAdapter (private val context: Context, private val engagementSDK: EngagementSDK?,
                                   private val session: LiveLikeContentSession?,
                                   private val interactionList:List<PredictionWidgetUserInteraction>
) : RecyclerView.Adapter<UnclaimedInteractionAdapter.InteractionViewHolder>()  {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UnclaimedInteractionAdapter.InteractionViewHolder {

        return InteractionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_layout,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: UnclaimedInteractionAdapter.InteractionViewHolder, position: Int) {
        val interaction = interactionList[position]
        engagementSDK?.fetchWidgetDetails(interaction.widgetId,interaction.widgetKind,object : LiveLikeCallback<LiveLikeWidget>(){
            @SuppressLint("SetTextI18n")
            override fun onResponse(
                result: LiveLikeWidget?,
                error: String?
            ) {
                if(result!= null){
                    Log.d("unclaimed","widget loaded")
                    holder.widgetKindTv.text = "Interacted Widget id:${result.id}"
                    holder.widgetIdTv.text = "Question: ${result.question}"

                    var selectedOptionId = interaction.optionId
                    var options = result.options
                    options?.forEach { option->
                        if(option?.id == selectedOptionId){
                            var optionName = option.description
                            holder.widgetAnswerTv.text = "Your answer was : ${optionName}"
                        }
                    }
                }
            }
        })



    }

    override fun getItemCount(): Int {
        return interactionList.size
    }

    inner class InteractionViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val widgetKindTv: TextView = view.findViewById(R.id.widgetKindTv)
        val widgetIdTv: TextView = view.findViewById(R.id.widgetIdTv)
        val widgetAnswerTv: TextView = view.findViewById(R.id.widgetAnswerTv)
        val widgetView: WidgetView = view.findViewById(R.id.widget_view)
    }
}