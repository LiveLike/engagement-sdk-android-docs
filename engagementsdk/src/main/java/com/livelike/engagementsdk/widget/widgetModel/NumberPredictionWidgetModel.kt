package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.core.data.models.NumberPredictionVotes
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.NumberPredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface NumberPredictionWidgetModel: LiveLikeWidgetMediator {

    /**
     * Locks the user's vote
     * @param options A List of vote submission for each options. All options must be submitted at the same time.
     */
    fun lockInVote(options:List<NumberPredictionVotes>)

    /**
     * Call this to load the latest user interaction for this widget
     */
    fun getUserInteraction(): NumberPredictionWidgetUserInteraction?

    /**
     * Call this to load the user's interaction history for this Widget
     */
    fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<NumberPredictionWidgetUserInteraction>>)
}