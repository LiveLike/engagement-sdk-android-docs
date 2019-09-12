package com.livelike.engagementsdk.widget

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.data.repository.UserRepository
import com.livelike.engagementsdk.widget.WidgetType.ALERT
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_POLL
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_PREDICTION
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_PREDICTION_FOLLOW_UP
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_QUIZ
import com.livelike.engagementsdk.widget.WidgetType.POINTS_TUTORIAL
import com.livelike.engagementsdk.widget.WidgetType.TEXT_POLL
import com.livelike.engagementsdk.widget.WidgetType.TEXT_PREDICTION
import com.livelike.engagementsdk.widget.WidgetType.TEXT_PREDICTION_FOLLOW_UP
import com.livelike.engagementsdk.widget.WidgetType.TEXT_QUIZ
import com.livelike.engagementsdk.widget.view.AlertWidgetView
import com.livelike.engagementsdk.widget.view.PollView
import com.livelike.engagementsdk.widget.view.PredictionView
import com.livelike.engagementsdk.widget.view.QuizView
import com.livelike.engagementsdk.widget.view.components.PointsTutorialView
import com.livelike.engagementsdk.widget.viewModel.AlertWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.PointTutorialWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.PollViewModel
import com.livelike.engagementsdk.widget.viewModel.PredictionViewModel
import com.livelike.engagementsdk.widget.viewModel.QuizViewModel
import com.livelike.engagementsdk.widget.viewModel.ViewModel

internal class WidgetProvider {
    fun get(
        widgetInfos: WidgetInfos,
        context: Context,
        analyticsService: AnalyticsService,
        sdkConfiguration: EngagementSDK.SdkConfiguration,
        onDismiss: () -> Unit,
        userRepository: UserRepository,
        programRepository: ProgramRepository
    ): SpecifiedWidgetView? {
        return when (WidgetType.fromString(widgetInfos.type)) {
            ALERT -> AlertWidgetView(context).apply {
                widgetViewModel = AlertWidgetViewModel(widgetInfos, analyticsService, onDismiss)
            }
            TEXT_QUIZ, IMAGE_QUIZ -> QuizView(context).apply {
                widgetViewModel = QuizViewModel(widgetInfos, analyticsService, sdkConfiguration, context, onDismiss, userRepository, programRepository)
            }
            IMAGE_PREDICTION, IMAGE_PREDICTION_FOLLOW_UP,
            TEXT_PREDICTION, TEXT_PREDICTION_FOLLOW_UP -> PredictionView(context).apply {
                widgetViewModel = PredictionViewModel(widgetInfos, context, analyticsService, onDismiss, userRepository, programRepository)
            }
            TEXT_POLL, IMAGE_POLL -> PollView(context).apply {
                widgetViewModel = PollViewModel(widgetInfos, analyticsService, sdkConfiguration, onDismiss, userRepository, programRepository)
            }
            POINTS_TUTORIAL -> PointsTutorialView(context).apply {
                widgetViewModel = PointTutorialWidgetViewModel(onDismiss, analyticsService)
            }
            else -> null
        }
    }
}

open class SpecifiedWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    open var widgetViewModel: ViewModel? = null
    open var dismissFunc: ((action: DismissAction) -> Unit)? = null
}
