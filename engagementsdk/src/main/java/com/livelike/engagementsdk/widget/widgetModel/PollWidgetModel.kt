package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface PollWidgetModel:LiveLikeWidgetMediator {
    val voteResults: Stream<LiveLikeWidgetResult>
    fun lockInAnswer(optionID: String)
}