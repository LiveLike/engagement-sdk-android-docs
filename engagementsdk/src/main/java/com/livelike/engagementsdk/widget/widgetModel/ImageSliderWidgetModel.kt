package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.CheerMeterUserInteraction
import com.livelike.engagementsdk.widget.data.models.EmojiSliderUserInteraction
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


    /**
     * it returns the latest user interaction for the widget
     */
    fun getUserInteraction() : EmojiSliderUserInteraction?

    /**
     * returns widget interaction from remote source
     */
    fun loadWidgetInteraction(liveLikeCallback: LiveLikeCallback<EmojiSliderUserInteraction>)



}