package com.mml.mmlengagementsdk.widgets.timeline

import android.content.Context
import android.view.View
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import com.livelike.engagementsdk.widget.widgetModel.FollowUpWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.ImageSliderWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PredictionWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel
import com.mml.mmlengagementsdk.widgets.MMLAlertWidget
import com.mml.mmlengagementsdk.widgets.MMLCheerMeterWidget
import com.mml.mmlengagementsdk.widgets.MMLImageSliderWidget
import com.mml.mmlengagementsdk.widgets.MMLPollWidget
import com.mml.mmlengagementsdk.widgets.MMLQuizWidget

class TimeLineWidgetFactory(
    val context: Context,
    private val widgetList: List<TimelineWidgetResource>
) : LiveLikeWidgetViewFactory {

    override fun createCheerMeterView(cheerMeterWidgetModel: CheerMeterWidgetmodel): View? {
        return MMLCheerMeterWidget(context).apply {
            this.cheerMeterWidgetModel = cheerMeterWidgetModel
            timelineWidgetResource = widgetList.find { it.liveLikeWidget.id == cheerMeterWidgetModel.widgetData.id }
        }
    }

    override fun createAlertWidgetView(alertWidgetModel: AlertWidgetModel): View? {
        return MMLAlertWidget(context).apply {
            this.alertModel = alertWidgetModel
            timelineWidgetResource = widgetList.find { it.liveLikeWidget.id == alertWidgetModel.widgetData.id }
        }
    }

    override fun createQuizWidgetView(
        quizWidgetModel: QuizWidgetModel,
        isImage: Boolean
    ): View? {
        return MMLQuizWidget(context).apply {
            this.isImage = isImage
            this.quizWidgetModel = quizWidgetModel
            timelineWidgetResource = widgetList.find { it.liveLikeWidget.id == quizWidgetModel.widgetData.id }
        }
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
        return MMLPollWidget(context).apply {
            this.isImage = isImage
            this.pollWidgetModel = pollWidgetModel
            timelineWidgetResource = widgetList.find { it.liveLikeWidget.id == pollWidgetModel.widgetData.id }
        }
    }

    override fun createImageSliderWidgetView(imageSliderWidgetModel: ImageSliderWidgetModel): View? {
        return MMLImageSliderWidget(context).apply {
            this.imageSliderWidgetModel = imageSliderWidgetModel
            timelineWidgetResource = widgetList.find { it.liveLikeWidget.id == imageSliderWidgetModel.widgetData.id }
        }
    }

    private fun isWidgetActive(liveLikeWidgetMediator: LiveLikeWidgetMediator): Boolean {
        return widgetList.find { it.liveLikeWidget.id == liveLikeWidgetMediator.widgetData.id }?.isActive
            ?: false
    }
}