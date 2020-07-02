package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.livelike.engagementsdk.ContentSession
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EngagementSDKTheme
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

    private var engagementSDKTheme: EngagementSDKTheme? = null
    private var widgetContainerViewModel: WidgetContainerViewModel? = null
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
            R.styleable.WidgetView,
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

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            val widgetType = state.getString("key")
            val widgetInfos = state.getParcelable<WidgetInfos>("value")
            if (widgetType != null && widgetInfos != null)
                widgetContainerViewModel?.currentWidgetViewStream?.onNext(
                    Pair(
                        widgetType,
                        (session as? ContentSession)?.createSpecifiedView(
                            context,
                            widgetInfos,
                            widgetViewThemeAttributes,
                            engagementSDKTheme
                        )
                    )
                )
            super.onRestoreInstanceState(state.getParcelable("super"))
        } else
            super.onRestoreInstanceState(state)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        val pair = widgetContainerViewModel?.currentWidgetViewStream?.latestNotNull()
        pair?.let {
            bundle.putString("key", pair.first)
            bundle.putParcelable("value", pair.second?.widgetInfos)
        }
        bundle.putParcelable("super", super.onSaveInstanceState())
        return bundle
    }

    private var session: LiveLikeContentSession? = null

    fun setSession(session: LiveLikeContentSession) {
        this.session = session
        session.setWidgetViewThemeAttribute(widgetViewThemeAttributes)
        session.setWidgetContainer(this, widgetViewThemeAttributes)
        session.analyticService.trackOrientationChange(resources.configuration.orientation == 1)
        widgetContainerViewModel = (session as ContentSession?)?.widgetContainer
        widgetContainerViewModel?.widgetLifeCycleEventsListener = widgetLifeCycleEventsListener
        session.widgetThemeStream.onNext(engagementSDKTheme?.widgets)
    }

    @Throws(Exception::class)
    internal fun setTheme(json: String) {
        val gson = Gson()
        engagementSDKTheme = gson.fromJson(json, EngagementSDKTheme::class.java)
        val validateString = engagementSDKTheme!!.validate()
        if (validateString != null) {
            throw Exception("$validateString")
        }
        (session as? ContentSession)?.widgetThemeStream?.onNext(engagementSDKTheme?.widgets)
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
            var widgetType = widgetResourceJson.get("kind").asString
            if (widgetType.contains("follow-up")) {
                widgetType = "$widgetType-updated"
            } else {
                widgetType = "$widgetType-created"
            }
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
                            widgetViewThemeAttributes,
                            engagementSDKTheme?.widgets
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
