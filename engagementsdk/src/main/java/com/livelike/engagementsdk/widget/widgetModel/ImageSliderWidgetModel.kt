package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface ImageSliderWidgetModel : LiveLikeWidgetMediator {


    /**
     * live stream for vote results
     */
    val voteResults: Stream<LiveLikeWidgetResult>


    /**
     * submits the value captured for image slider
     */
    fun lockInVote(magnitude: Double)

}