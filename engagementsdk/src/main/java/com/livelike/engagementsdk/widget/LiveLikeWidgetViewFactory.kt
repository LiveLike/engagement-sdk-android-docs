package com.livelike.engagementsdk.widget

import android.view.View
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel

interface LiveLikeWidgetViewFactory {


    fun createCheerMeterView(
        cheerMeterWidgetModel: CheerMeterWidgetmodel
    ): View?

    fun createAlertWidgetView(
        alertWidgetModel: AlertWidgetModel
    ): View?

    fun createQuizWidgetView(
        quizWidgetModel: QuizWidgetModel, isImage: Boolean
    ): View?

    fun createPollWidgetView(
        pollWidgetModel: PollWidgetModel, isImage: Boolean
    ): View?


}