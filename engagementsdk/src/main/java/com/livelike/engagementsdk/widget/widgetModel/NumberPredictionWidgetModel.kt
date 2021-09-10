package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.core.data.models.NumberPredictionVotes
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.NumberPredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface NumberPredictionWidgetModel: LiveLikeWidgetMediator {

    /**
     * submits the answer for prediction
     */
    fun lockInVote(options:List<NumberPredictionVotes>)

    /**
     * it returns the latest user interaction for the widget
     */
    fun getUserInteraction(): NumberPredictionWidgetUserInteraction?

    /**
     * returns widget interactions from remote source
     */
    fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<NumberPredictionWidgetUserInteraction>>)
}