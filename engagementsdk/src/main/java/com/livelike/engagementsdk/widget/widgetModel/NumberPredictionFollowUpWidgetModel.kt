package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.core.data.models.NumberPredictionVotes
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.NumberPredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface NumberPredictionFollowUpWidgetModel: LiveLikeWidgetMediator {

    /**
     * returns the predicted scores which was voted previously
     **/
    fun getPredictionVotes(): List<NumberPredictionVotes>?

    /**
     * claim earned rewards if any for the chosen prediction
     **/
    fun claimRewards()
    
    /**
     * it returns the latest user interaction for the widget
     */
    fun getUserInteraction(): NumberPredictionWidgetUserInteraction?

    /**
     * returns widget interactions from remote source
     */
    fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<NumberPredictionWidgetUserInteraction>>)
}