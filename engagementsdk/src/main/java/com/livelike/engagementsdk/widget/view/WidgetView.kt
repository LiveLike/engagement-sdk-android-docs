package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.gson.JsonObject
import com.livelike.engagementsdk.ContentSession
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.MockAnalyticsService
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetProvider
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.viewModel.WidgetContainerViewModel

class WidgetView(context: Context, private val attr: AttributeSet) : FrameLayout(context, attr) {

    private var widgetContainerViewModel: WidgetContainerViewModel? =
        WidgetContainerViewModel(SubscriptionManager())
    private val widgetViewThemeAttributes = WidgetViewThemeAttributes()

    var widgetLifeCycleEventsListener: WidgetLifeCycleEventsListener? = null
        set(value) {
            field = value
            widgetContainerViewModel?.widgetLifeCycleEventsListener = value
        }

    var enableDefaultWidgetTransition = true
        set(value) {
            field = value
            widgetContainerViewModel?.enableDefaultWidgetTransition = value
        }

    init {
        context.obtainStyledAttributes(
            attr,
            R.styleable.LiveLike_WidgetView,
            0, 0
        ).apply {
            try {
                widgetViewThemeAttributes.init(this)
            } finally {
                recycle()
            }
        }
        widgetContainerViewModel?.setWidgetContainer(this, widgetViewThemeAttributes)
    }

    fun setSession(session: LiveLikeContentSession) {
        session.setWidgetViewThemeAttribute(widgetViewThemeAttributes)
        session.setWidgetContainer(this, widgetViewThemeAttributes)
        session.analyticService.trackOrientationChange(resources.configuration.orientation == 1)
        widgetContainerViewModel = (session as ContentSession?)?.widgetContainer
        widgetContainerViewModel?.widgetLifeCycleEventsListener = widgetLifeCycleEventsListener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthDp = AndroidResource.pxToDp(width)
        if (widthDp < 292 && widthDp != 0) {
            logError { "[CONFIG ERROR] Current WidgetView Width is $widthDp, it must be more than 292dp or won't display on the screen." }
            setMeasuredDimension(0, 0)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /** displays the widget in the container
    throws error if json invalid
    clears the previous displayed widget (if any)
    only clears if json is valid
     */
    fun displayWidget(sdk: EngagementSDK, widgetResourceJson: JsonObject) {
        try {
            val widgetType = widgetResourceJson.get("kind").asString + "-created"
            val widgetId = widgetResourceJson["id"].asString
            widgetContainerViewModel?.currentWidgetViewStream?.onNext(
                Pair(
                    widgetType,
                    WidgetProvider()
                        .get(
                            null,
                            WidgetInfos(widgetType, widgetResourceJson, widgetId),
                            context,
                            MockAnalyticsService(),
                            sdk.configurationStream.latest()!!,
                            {
                            },
                            sdk.userRepository,
                            null,
                            SubscriptionManager(),
                            widgetViewThemeAttributes
                        )
                )
            )
        } catch (ex: Exception) {
            logDebug { "Invalid json passed for displayWidget" }
            ex.printStackTrace()
        }
    }

    // clears the displayed widget (if any)
    fun clearWidget() {
        removeAllViews()
    }

    fun moveToNextState() {
        if (childCount == 1 && getChildAt(0) is SpecifiedWidgetView) {
            (getChildAt(0) as SpecifiedWidgetView).moveToNextState()
        }
    }
}
