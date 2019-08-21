package com.livelike.livelikesdk.widget

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.DismissAction
import com.livelike.livelikesdk.EngagementSDK
import com.livelike.livelikesdk.WidgetInfos
import com.livelike.livelikesdk.widget.WidgetType.ALERT
import com.livelike.livelikesdk.widget.WidgetType.IMAGE_POLL
import com.livelike.livelikesdk.widget.WidgetType.IMAGE_PREDICTION
import com.livelike.livelikesdk.widget.WidgetType.IMAGE_PREDICTION_FOLLOW_UP
import com.livelike.livelikesdk.widget.WidgetType.IMAGE_QUIZ
import com.livelike.livelikesdk.widget.WidgetType.POINTS_TUTORIAL
import com.livelike.livelikesdk.widget.WidgetType.TEXT_POLL
import com.livelike.livelikesdk.widget.WidgetType.TEXT_PREDICTION
import com.livelike.livelikesdk.widget.WidgetType.TEXT_PREDICTION_FOLLOW_UP
import com.livelike.livelikesdk.widget.WidgetType.TEXT_QUIZ
import com.livelike.livelikesdk.widget.view.AlertWidgetView
import com.livelike.livelikesdk.widget.view.PollView
import com.livelike.livelikesdk.widget.view.PredictionView
import com.livelike.livelikesdk.widget.view.QuizView
import com.livelike.livelikesdk.widget.view.components.PointsTutorialView
import com.livelike.livelikesdk.widget.viewModel.AlertWidgetViewModel
import com.livelike.livelikesdk.widget.viewModel.PointTutorialWidgetViewModel
import com.livelike.livelikesdk.widget.viewModel.PollViewModel
import com.livelike.livelikesdk.widget.viewModel.PredictionViewModel
import com.livelike.livelikesdk.widget.viewModel.QuizViewModel
import com.livelike.livelikesdk.widget.viewModel.WidgetViewModel

internal class WidgetProvider {
    fun get(
        widgetInfos: WidgetInfos,
        context: Context,
        analyticsService: AnalyticsService,
        sdkConfiguration: EngagementSDK.SdkConfiguration,
        onDismiss: () -> Unit
    ): SpecifiedWidgetView? {
        return when (WidgetType.fromString(widgetInfos.type)) {
            ALERT -> AlertWidgetView(context).apply {
                widgetViewModel = AlertWidgetViewModel(widgetInfos, analyticsService, onDismiss)
            }
            TEXT_QUIZ, IMAGE_QUIZ -> QuizView(context).apply {
                widgetViewModel = QuizViewModel(widgetInfos, analyticsService, sdkConfiguration, context, onDismiss)
            }
            IMAGE_PREDICTION, IMAGE_PREDICTION_FOLLOW_UP,
            TEXT_PREDICTION, TEXT_PREDICTION_FOLLOW_UP -> PredictionView(context).apply {
                widgetViewModel = PredictionViewModel(widgetInfos, context, analyticsService, onDismiss)
            }
            TEXT_POLL, IMAGE_POLL -> PollView(context).apply {
                widgetViewModel = PollViewModel(widgetInfos, analyticsService, sdkConfiguration, onDismiss)
            }
            POINTS_TUTORIAL -> PointsTutorialView(context).apply {
                widgetViewModel = PointTutorialWidgetViewModel(onDismiss)
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
    open var widgetViewModel: WidgetViewModel? = null
    open var dismissFunc: ((action: DismissAction) -> Unit)? = null
}
