package com.livelike.engagementsdk.widget.widgetModel

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface QuizWidgetModel : LiveLikeWidgetMediator {


    /**
    * live stream for vote results
    */
    val voteResults: Stream<LiveLikeWidgetResult>

    /**
     * lock the answer for quiz
     */
    fun lockInAnswer(optionID: String)

}