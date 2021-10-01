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
     * Attempts to claim rewards for this Number Prediction Follow Up
     **/
    fun claimRewards()

    /**
     * Call this to load the latest user interaction for this widget
     */
    fun getUserInteraction(): NumberPredictionWidgetUserInteraction?

    /**
     * Call this to load the user's interaction history for this Widget
     */
    fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<NumberPredictionWidgetUserInteraction>>)
}