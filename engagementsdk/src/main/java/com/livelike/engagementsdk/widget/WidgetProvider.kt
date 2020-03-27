package com.livelike.engagementsdk.widget

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.data.models.Badge
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.data.repository.UserRepository
import com.livelike.engagementsdk.services.messaging.proxies.LiveLikeWidgetEntity
import com.livelike.engagementsdk.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.widget.WidgetType.ALERT
import com.livelike.engagementsdk.widget.WidgetType.CHEER_METER
import com.livelike.engagementsdk.widget.WidgetType.COLLECT_BADGE
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_POLL
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_PREDICTION
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_PREDICTION_FOLLOW_UP
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_QUIZ
import com.livelike.engagementsdk.widget.WidgetType.IMAGE_SLIDER
import com.livelike.engagementsdk.widget.WidgetType.POINTS_TUTORIAL
import com.livelike.engagementsdk.widget.WidgetType.TEXT_POLL
import com.livelike.engagementsdk.widget.WidgetType.TEXT_PREDICTION
import com.livelike.engagementsdk.widget.WidgetType.TEXT_PREDICTION_FOLLOW_UP
import com.livelike.engagementsdk.widget.WidgetType.TEXT_QUIZ
import com.livelike.engagementsdk.widget.view.AlertWidgetView
import com.livelike.engagementsdk.widget.view.CheerMeterView
import com.livelike.engagementsdk.widget.view.CollectBadgeWidgetView
import com.livelike.engagementsdk.widget.view.EmojiSliderWidgetView
import com.livelike.engagementsdk.widget.view.PollView
import com.livelike.engagementsdk.widget.view.PredictionView
import com.livelike.engagementsdk.widget.view.QuizView
import com.livelike.engagementsdk.widget.view.components.PointsTutorialView
import com.livelike.engagementsdk.widget.viewModel.AlertWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.CheerMeterViewModel
import com.livelike.engagementsdk.widget.viewModel.CollectBadgeWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.EmojiSliderWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.PointTutorialWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.PollViewModel
import com.livelike.engagementsdk.widget.viewModel.PredictionViewModel
import com.livelike.engagementsdk.widget.viewModel.QuizViewModel
import com.livelike.engagementsdk.widget.viewModel.ViewModel

internal class WidgetProvider {
    fun get(
        widgetMessagingClient: WidgetManager,
        widgetInfos: WidgetInfos,
        context: Context,
        analyticsService: AnalyticsService,
        sdkConfiguration: EngagementSDK.SdkConfiguration,
        onDismiss: () -> Unit,
        userRepository: UserRepository,
        programRepository: ProgramRepository,
        animationEventsStream: SubscriptionManager<ViewAnimationEvents>,
        widgetThemeAttributes: WidgetViewThemeAttributes
    ): SpecifiedWidgetView? {
        val specifiedWidgetView = when (WidgetType.fromString(widgetInfos.type)) {
            ALERT -> AlertWidgetView(context).apply {
                widgetViewModel = AlertWidgetViewModel(widgetInfos, analyticsService, onDismiss)
            }
            TEXT_QUIZ, IMAGE_QUIZ -> QuizView(context).apply {
                widgetViewThemeAttributes = widgetThemeAttributes
                widgetViewModel = QuizViewModel(
                    widgetInfos,
                    analyticsService,
                    sdkConfiguration,
                    context,
                    onDismiss,
                    userRepository,
                    programRepository,
                    widgetMessagingClient
                )
            }
            IMAGE_PREDICTION, IMAGE_PREDICTION_FOLLOW_UP,
            TEXT_PREDICTION, TEXT_PREDICTION_FOLLOW_UP -> PredictionView(context).apply {
                widgetViewThemeAttributes = widgetThemeAttributes
                widgetViewModel = PredictionViewModel(
                    widgetInfos,
                    context,
                    analyticsService,
                    sdkConfiguration,
                    onDismiss,
                    userRepository,
                    programRepository,
                    widgetMessagingClient
                )
            }
            TEXT_POLL, IMAGE_POLL -> PollView(context).apply {
                widgetViewThemeAttributes = widgetThemeAttributes
                widgetViewModel = PollViewModel(
                    widgetInfos,
                    analyticsService,
                    sdkConfiguration,
                    onDismiss,
                    userRepository,
                    programRepository,
                    widgetMessagingClient
                )
            }
            POINTS_TUTORIAL -> PointsTutorialView(context).apply {
                widgetViewModel = PointTutorialWidgetViewModel(
                    onDismiss,
                    analyticsService,
                    programRepository.rewardType,
                    programRepository.programGamificationProfileStream.latest()
                )
            }
            COLLECT_BADGE -> CollectBadgeWidgetView(context).apply {
                widgetViewModel = CollectBadgeWidgetViewModel(
                    gson.fromJson(
                        widgetInfos.payload,
                        Badge::class.java
                    ), onDismiss, analyticsService, animationEventsStream
                )
            }
            CHEER_METER -> CheerMeterView(context).apply {
                widgetViewThemeAttributes = widgetThemeAttributes
                widgetViewModel = CheerMeterViewModel(
                    widgetInfos,
                    analyticsService,
                    sdkConfiguration,
                    onDismiss,
                    userRepository,
                    programRepository,
                    widgetMessagingClient
                )
            }
            IMAGE_SLIDER -> EmojiSliderWidgetView(context).apply {
                widgetViewModel = EmojiSliderWidgetViewModel(
                    widgetInfos, analyticsService, sdkConfiguration, onDismiss,
                    userRepository, programRepository, widgetMessagingClient
                )
            }
            else -> null
        }
        specifiedWidgetView?.widgetId = widgetInfos.widgetId
        specifiedWidgetView?.widgetInfos = widgetInfos
        return specifiedWidgetView
    }
}

open class SpecifiedWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var widgetId: String = ""
    lateinit var widgetInfos: WidgetInfos
    open var widgetViewModel: ViewModel? = null
    open var dismissFunc: ((action: DismissAction) -> Unit)? = null
    open var widgetViewThemeAttributes: WidgetViewThemeAttributes = WidgetViewThemeAttributes()

    var widgetLifeCycleEventsListener: WidgetLifeCycleEventsListener? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetLifeCycleEventsListener?.onWidgetPresented(gson.fromJson(widgetInfos.payload.toString(), LiveLikeWidgetEntity::class.java))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        widgetLifeCycleEventsListener?.onWidgetDismissed(gson.fromJson(widgetInfos.payload.toString(), LiveLikeWidgetEntity::class.java))
    }

    fun onWidgetInteractionCompleted() {
        widgetLifeCycleEventsListener?.onWidgetInteractionCompleted(gson.fromJson(widgetInfos.payload.toString(), LiveLikeWidgetEntity::class.java))
    }
}
