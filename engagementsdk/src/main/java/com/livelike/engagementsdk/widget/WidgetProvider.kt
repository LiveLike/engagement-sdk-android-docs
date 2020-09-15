package com.livelike.engagementsdk.widget

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.FontFamilyProvider
import com.livelike.engagementsdk.LiveLikeEngagementTheme
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.messaging.proxies.LiveLikeWidgetEntity
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
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
import com.livelike.engagementsdk.widget.data.models.Badge
import com.livelike.engagementsdk.widget.view.AlertWidgetView
import com.livelike.engagementsdk.widget.view.CheerMeterView
import com.livelike.engagementsdk.widget.view.CollectBadgeWidgetView
import com.livelike.engagementsdk.widget.view.EmojiSliderWidgetView
import com.livelike.engagementsdk.widget.view.PollView
import com.livelike.engagementsdk.widget.view.PredictionView
import com.livelike.engagementsdk.widget.view.QuizView
import com.livelike.engagementsdk.widget.view.components.EggTimerCloseButtonView
import com.livelike.engagementsdk.widget.view.components.PointsTutorialView
import com.livelike.engagementsdk.widget.viewModel.AlertWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.CheerMeterViewModel
import com.livelike.engagementsdk.widget.viewModel.CollectBadgeWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.EmojiSliderWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.PointTutorialWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.PollViewModel
import com.livelike.engagementsdk.widget.viewModel.PredictionViewModel
import com.livelike.engagementsdk.widget.viewModel.QuizViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.txtTitleBackground
import kotlin.math.min

internal class WidgetProvider {
    fun get(
        widgetMessagingClient: WidgetManager? = null,
        widgetInfos: WidgetInfos,
        context: Context,
        analyticsService: AnalyticsService,
        sdkConfiguration: EngagementSDK.SdkConfiguration,
        onDismiss: () -> Unit,
        userRepository: UserRepository,
        programRepository: ProgramRepository? = null,
        animationEventsStream: SubscriptionManager<ViewAnimationEvents>,
        widgetThemeAttributes: WidgetViewThemeAttributes,
        liveLikeEngagementTheme: LiveLikeEngagementTheme?
    ): SpecifiedWidgetView? {
        val specifiedWidgetView = when (WidgetType.fromString(widgetInfos.type)) {
            ALERT -> AlertWidgetView(context).apply {
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = AlertWidgetViewModel(widgetInfos, analyticsService, onDismiss)
            }
            TEXT_QUIZ, IMAGE_QUIZ -> QuizView(context).apply {
                widgetViewThemeAttributes = widgetThemeAttributes
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
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
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
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
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
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
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = PointTutorialWidgetViewModel(
                    onDismiss,
                    analyticsService,
                    programRepository?.rewardType ?: RewardsType.NONE,
                    programRepository?.programGamificationProfileStream?.latest()
                )
            }
            COLLECT_BADGE -> CollectBadgeWidgetView(context).apply {
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = CollectBadgeWidgetViewModel(
                    gson.fromJson(
                        widgetInfos.payload,
                        Badge::class.java
                    ), onDismiss, analyticsService, animationEventsStream
                )
            }
            CHEER_METER -> CheerMeterView(context).apply {
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
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
                this.widgetsTheme = liveLikeEngagementTheme?.widgets
                this.fontFamilyProvider = liveLikeEngagementTheme?.fontFamilyProvider
                widgetViewModel = EmojiSliderWidgetViewModel(
                    widgetInfos, analyticsService, sdkConfiguration, onDismiss,
                    userRepository, programRepository, widgetMessagingClient
                )
            }
            else -> null
        }
        logDebug { "Widget created from provider, type: ${WidgetType.fromString(widgetInfos.type)}" }
        specifiedWidgetView?.widgetId = widgetInfos.widgetId
        specifiedWidgetView?.widgetInfos = widgetInfos
        return specifiedWidgetView
    }
}

abstract class SpecifiedWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    internal var fontFamilyProvider: FontFamilyProvider? = null

    var widgetId: String = ""
    lateinit var widgetInfos: WidgetInfos
    open var widgetViewModel: BaseViewModel? = null
    open var dismissFunc: ((action: DismissAction) -> Unit)? = null
    open var widgetViewThemeAttributes: WidgetViewThemeAttributes = WidgetViewThemeAttributes()
    open var widgetsTheme: WidgetsTheme? = null

    var widgetLifeCycleEventsListener: WidgetLifeCycleEventsListener? = null

    lateinit var widgetData: LiveLikeWidgetEntity

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetData =
            gson.fromJson(widgetInfos.payload.toString(), LiveLikeWidgetEntity::class.java)
        postDelayed({
            widgetData.height = height
            widgetLifeCycleEventsListener?.onWidgetPresented(widgetData)
        }, 500)
        subscribeWidgetStateAndPublishToLifecycleListener()
    }

    private fun subscribeWidgetStateAndPublishToLifecycleListener() {
        widgetViewModel?.widgetState?.subscribe(this) {
            it?.let {
                widgetLifeCycleEventsListener?.onWidgetStateChange(it, widgetData)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        widgetLifeCycleEventsListener?.onWidgetDismissed(widgetData)
    }

    fun onWidgetInteractionCompleted() {
        widgetLifeCycleEventsListener?.onWidgetInteractionCompleted(widgetData)
    }

    internal fun showTimer(
        time: String,
        animationEggTimerProgress: Float?,
        v: EggTimerCloseButtonView?,
        onUpdate: (Float) -> Unit,
        dismissAction: (action: DismissAction) -> Unit
    ) {
        if (widgetViewModel?.enableDefaultWidgetTransition == false) {
            v?.visibility = View.GONE
            return
        }
        val animationLength = AndroidResource.parseDuration(time).toFloat()
        if ((animationEggTimerProgress ?: 0f) < 1f) {
            animationEggTimerProgress?.let {
                v?.startAnimationFrom(it, animationLength, onUpdate, dismissAction)
            }
        }
    }

    protected fun applyThemeOnTitleView(it: WidgetBaseThemeComponent) {
        titleView.componentTheme = it.title
        AndroidResource.updateThemeForView(titleTextView, it.title, fontFamilyProvider)
        if (it.header?.background != null) {
            txtTitleBackground.background = AndroidResource.createDrawable(it.header)
        }
        AndroidResource.setPaddingForView(txtTitleBackground, it.header?.padding)
    }

    /**
     * override this method in respective widgets to respect runtime unified json theme updation
     **/
    open fun applyTheme(theme: WidgetsTheme) {
        widgetsTheme = theme
    }

    fun applyTheme(theme: LiveLikeEngagementTheme) {
        fontFamilyProvider = theme.fontFamilyProvider
        applyTheme(theme.widgets)
    }

    open fun moveToNextState() {
        val nextStateOrdinal = (widgetViewModel?.widgetState?.latest()?.ordinal ?: 0) + 1
        widgetViewModel?.widgetState?.onNext(
            WidgetStates.values()[min(
                nextStateOrdinal,
                WidgetStates.FINISHED.ordinal
            )]
        )
    }
}
