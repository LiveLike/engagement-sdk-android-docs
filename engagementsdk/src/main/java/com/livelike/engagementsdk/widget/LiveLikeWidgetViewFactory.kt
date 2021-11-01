package com.livelike.engagementsdk.widget

import android.view.View
import com.livelike.engagementsdk.widget.widgetModel.*

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
        quizWidgetModel: QuizWidgetModel,
        isImage: Boolean
    ): View?

    fun createPredictionWidgetView(
        predictionViewModel: PredictionWidgetViewModel,
        isImage: Boolean
    ): View?

    fun createPredictionFollowupWidgetView(
        followUpWidgetViewModel: FollowUpWidgetViewModel,
        isImage: Boolean
    ): View?

    fun createPollWidgetView(
        pollWidgetModel: PollWidgetModel,
        isImage: Boolean
    ): View?

    fun createImageSliderWidgetView(
        imageSliderWidgetModel: ImageSliderWidgetModel
    ): View?

    fun createVideoAlertWidgetView(
        videoAlertWidgetModel: VideoAlertWidgetModel
    ): View?

    fun createTextAskWidgetView(
        imageSliderWidgetModel: TextAskWidgetModel
    ): View?

    fun createNumberPredictionWidgetView(
        numberPredictionWidgetModel: NumberPredictionWidgetModel,
        isImage: Boolean
    ): View?

    fun createNumberPredictionFollowupWidgetView(
        followUpWidgetViewModel: NumberPredictionFollowUpWidgetModel,
        isImage: Boolean
    ): View?

    fun createSocialEmbedWidgetView(
        socialEmbedWidgetModel: SocialEmbedWidgetModel
    ): View?
}
