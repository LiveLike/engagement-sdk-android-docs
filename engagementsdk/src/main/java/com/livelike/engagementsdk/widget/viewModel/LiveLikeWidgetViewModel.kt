package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult

interface LiveLikeWidgetMediator {

    val widgetData : LiveLikeWidget

    val voteResults : Stream<LiveLikeWidgetResult>

    fun submitVote(optionID: String)

    fun dismissWidget(action: DismissAction)

}