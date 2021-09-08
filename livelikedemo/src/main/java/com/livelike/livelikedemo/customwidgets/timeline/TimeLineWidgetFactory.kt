package com.livelike.livelikedemo.customwidgets.timeline

import MMLAlertWidget
import MMLPollWidget
import android.content.Context
import android.view.View
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.timeline.TimelineWidgetResource
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import com.livelike.engagementsdk.widget.widgetModel.FollowUpWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.ImageSliderWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.NumberPredictionWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PredictionWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.TextAskWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.VideoAlertWidgetModel

class TimeLineWidgetFactory(
    val context: Context,
    private val widgetList: List<TimelineWidgetResource>?
) : LiveLikeWidgetViewFactory {

    override fun createCheerMeterView(cheerMeterWidgetModel: CheerMeterWidgetmodel): View? {
        return null
    }

    override fun createAlertWidgetView(alertWidgetModel: AlertWidgetModel): View? {
        return MMLAlertWidget(
            context, alertWidgetModel,
            widgetList?.find { it.liveLikeWidget.id == alertWidgetModel.widgetData.id }, isWidgetActive(alertWidgetModel)
        )
    }

    override fun createQuizWidgetView(
        quizWidgetModel: QuizWidgetModel,
        isImage: Boolean
    ): View? {
        return null
    }

    override fun createPredictionWidgetView(
        predictionViewModel: PredictionWidgetViewModel,
        isImage: Boolean
    ): View? {
        return null
    }

    override fun createPredictionFollowupWidgetView(
        followUpWidgetViewModel: FollowUpWidgetViewModel,
        isImage: Boolean
    ): View? {
        return null
    }

    override fun createPollWidgetView(
        pollWidgetModel: PollWidgetModel,
        isImage: Boolean
    ): View? {
        return MMLPollWidget(
            context,
            pollWidgetModel,
            widgetList?.find {
                it.liveLikeWidget.id == pollWidgetModel.widgetData.id
            },
            isImage,
            isWidgetActive(pollWidgetModel)
        )
    }

    override fun createImageSliderWidgetView(imageSliderWidgetModel: ImageSliderWidgetModel): View? {
        return null
    }

    override fun createVideoAlertWidgetView(videoAlertWidgetModel: VideoAlertWidgetModel): View? {
        return null
    }

    override fun createTextAskWidgetView(imageSliderWidgetModel: TextAskWidgetModel): View? {
        return null
    }

    override fun createNumberPredictionWidgetView(numberPredictionWidgetModel: NumberPredictionWidgetModel): View? {
        return null
    }


    private fun isWidgetActive(liveLikeWidgetMediator: LiveLikeWidgetMediator): Boolean {
        return widgetList?.find { it.liveLikeWidget.id == liveLikeWidgetMediator.widgetData.id }?.widgetState == WidgetStates.INTERACTING
    }
}
