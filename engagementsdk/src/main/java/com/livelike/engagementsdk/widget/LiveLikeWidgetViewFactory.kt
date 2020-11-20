package com.livelike.engagementsdk.widget

import android.view.View
import com.livelike.engagementsdk.widget.viewModel.AlertWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel

/**
 * WidgetView Factory is responsible for providing the instance of custom widget ui.
 **/

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


}