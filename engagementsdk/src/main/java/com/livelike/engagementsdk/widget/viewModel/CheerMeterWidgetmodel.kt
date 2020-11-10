package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult

interface CheerMeterWidgetmodel : LiveLikeWidgetMediator {

    val voteResults : Stream<LiveLikeWidgetResult>

    fun submitVote(optionID: String)

}