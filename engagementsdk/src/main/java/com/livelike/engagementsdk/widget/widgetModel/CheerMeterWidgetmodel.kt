package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface CheerMeterWidgetmodel : LiveLikeWidgetMediator {

    val voteResults : Stream<LiveLikeWidgetResult>

    fun submitVote(optionID: String)

}