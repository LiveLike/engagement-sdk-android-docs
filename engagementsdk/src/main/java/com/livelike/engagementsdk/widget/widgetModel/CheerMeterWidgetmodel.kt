package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface CheerMeterWidgetmodel : LiveLikeWidgetMediator {

    /**
     * live stream for vote results
     */
    val voteResults : Stream<LiveLikeWidgetResult>

    /**
     * record the cheer vote
     */
    fun submitVote(optionID: String)

}