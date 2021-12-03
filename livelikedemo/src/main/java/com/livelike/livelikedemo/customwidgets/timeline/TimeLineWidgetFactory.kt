package com.livelike.livelikedemo.customwidgets.timeline

import MMLAlertWidget
import MMLPollWidget
import android.content.Context
import android.view.View
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.timeline.TimelineWidgetResource
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import com.livelike.engagementsdk.widget.widgetModel.*
import com.livelike.livelikedemo.customwidgets.CustomNumberPredictionWidget

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

    override fun createNumberPredictionWidgetView(numberPredictionWidgetModel: NumberPredictionWidgetModel,
                                                  isImage: Boolean): View? {
        return CustomNumberPredictionWidget(context).apply {
            this.numberPredictionWidgetViewModel = numberPredictionWidgetModel
            this.isImage = isImage
        }
    }

    override fun createNumberPredictionFollowupWidgetView(
        followUpWidgetViewModel: NumberPredictionFollowUpWidgetModel,
        isImage: Boolean
    ): View? {
        return CustomNumberPredictionWidget(context).apply {
            this.followUpWidgetViewModel = followUpWidgetViewModel
            this.isImage = isImage
            this.isFollowUp = true
        }
    }

    override fun createSocialEmbedWidgetView(socialEmbedWidgetModel: SocialEmbedWidgetModel): View? {
        return null
    }


    private fun isWidgetActive(liveLikeWidgetMediator: LiveLikeWidgetMediator): Boolean {
        return widgetList?.find { it.liveLikeWidget.id == liveLikeWidgetMediator.widgetData.id }?.widgetState == WidgetStates.INTERACTING
    }
}
