package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream

interface LiveLikeWidgetMediator {


    val voteResults : Stream<LiveLikeWidget>

    fun submitVote(optionID: String)

    fun dismissWidget(action: DismissAction)

}