package com.livelike.engagementsdk.widget.data.respository

import android.content.Context
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.widget.data.models.CheerMeterUserInteraction
import com.livelike.engagementsdk.widget.data.models.UserWidgetInteractionApi
import com.livelike.engagementsdk.widget.data.models.WidgetKind
import com.livelike.engagementsdk.widget.data.models.WidgetUserInteractionBase

/**
 * Repository that handles user's widget interaction data. It knows what data sources need to be
 * triggered to get widget interaction and where to store the data.
 **/
internal class WidgetInteractionRepository(val context: Context) {


    private val widgetInteractionRemoteSource: WidgetInteractionRemoteSource = WidgetInteractionRemoteSource()

    private val widgetInteractionMap = mutableMapOf<String, WidgetUserInteractionBase>()

    fun <T : WidgetUserInteractionBase> saveWidgetInteraction(widgetInteraction :T){
        widgetInteractionMap[widgetInteraction.widgetId] = widgetInteraction
    }

    fun <T : WidgetUserInteractionBase> getWidgetInteraction(
        widgetId: String,
        widgetKind: WidgetKind
    ): T? {
        return widgetInteractionMap[widgetId] as T?
    }

    internal suspend fun fetchAndStoreWidgetInteractions(url: String, accessToken: String): Result<UserWidgetInteractionApi> {

        val widgetInteractionsResult =
            widgetInteractionRemoteSource.getWidgetInteractions(url, accessToken)

        if (widgetInteractionsResult is com.livelike.engagementsdk.core.services.network.Result.Success) {
            val interactionList = mutableListOf<WidgetUserInteractionBase>()
            widgetInteractionsResult.data.interactions?.let { interactions ->
                interactions.cheerMeter?.let { interactionList.addAll(it) }
                interactions.emojiSlider?.let { interactionList.addAll(it) }
                interactions.textPoll?.let { interactionList.addAll(it) }
                interactions.textPrediction?.let { interactionList.addAll(it) }
                interactions.textQuiz?.let { interactionList.addAll(it) }
                interactions.imagePoll?.let { interactionList.addAll(it) }
                interactions.imagePrediction?.let { interactionList.addAll(it) }
                interactions.imageQuiz?.let { interactionList.addAll(it) }
            }
            interactionList.forEach {
                if (it is CheerMeterUserInteraction && widgetInteractionMap[it.widgetId] != null) {
                    it.totalScore += (widgetInteractionMap[it.widgetId] as CheerMeterUserInteraction).totalScore
                }
                widgetInteractionMap[it.widgetId] = it
            }
        }
        return  widgetInteractionsResult
    }

}