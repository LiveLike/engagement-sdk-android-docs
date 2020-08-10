package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.FrameLayout
<<<<<<< Updated upstream
import com.google.gson.JsonObject
=======
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
>>>>>>> Stashed changes
import com.livelike.engagementsdk.ContentSession
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeEngagementTheme
<<<<<<< Updated upstream
import com.livelike.engagementsdk.MockAnalyticsService
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.WidgetInfos
=======
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.MockAnalyticsService
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.WidgetListener
>>>>>>> Stashed changes
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetProvider
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.viewModel.WidgetContainerViewModel

class WidgetView(context: Context, private val attr: AttributeSet) : FrameLayout(context, attr) {

    private var engagementSDKTheme: LiveLikeEngagementTheme? = null
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

    private var session: LiveLikeContentSession? = null

    fun setSession(session: LiveLikeContentSession) {
        this.session = session
        session.setWidgetViewThemeAttribute(widgetViewThemeAttributes)
        session.setWidgetContainer(this, widgetViewThemeAttributes)
        session.analyticService.trackOrientationChange(resources.configuration.orientation == 1)
        widgetContainerViewModel = (session as ContentSession?)?.widgetContainer
        widgetContainerViewModel?.widgetLifeCycleEventsListener = widgetLifeCycleEventsListener
        session.livelikeThemeStream.onNext(engagementSDKTheme)
<<<<<<< Updated upstream
=======
        session.widgetStream.subscribe(this) {
            it?.let {
                widgetListener?.onNewWidget(it)
            }
        }
>>>>>>> Stashed changes
    }

    /**
     * will update the value of theme to be applied for all widgets
     * This will update the theme on the current displayed widget as well
     **/
    fun applyTheme(theme: LiveLikeEngagementTheme) {
        engagementSDKTheme = theme
        (session as? ContentSession)?.livelikeThemeStream?.onNext(engagementSDKTheme)
        if (childCount == 1 && getChildAt(0) is SpecifiedWidgetView) {
            (getChildAt(0) as SpecifiedWidgetView).applyTheme(theme)
        }
<<<<<<< Updated upstream
    }

    /**
     * this method parse livelike theme from json object and apply if its a valid json
     * refer @applyTheme(theme)
     **/
    fun applyTheme(themeJson: JsonObject): Result<Boolean> {
        val themeResult = LiveLikeEngagementTheme.instanceFrom(themeJson)
        return if (themeResult is Result.Success) {
            applyTheme(themeResult.data)
            Result.Success(true)
        } else {
            themeResult as Result.Error
        }
    }

=======
    }

    /**
     * this method parse livelike theme from json object and apply if its a valid json
     * refer @applyTheme(theme)
     **/
    fun applyTheme(themeJson: JsonObject): Result<Boolean> {
        val themeResult = LiveLikeEngagementTheme.instanceFrom(themeJson)
        return if (themeResult is Result.Success) {
            applyTheme(themeResult.data)
            Result.Success(true)
        } else {
            themeResult as Result.Error
        }
    }

>>>>>>> Stashed changes
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthDp = AndroidResource.pxToDp(width)
        if (widthDp < 292 && widthDp != 0) {
            logError { "[CONFIG ERROR] Current WidgetView Width is $widthDp, it must be more than 292dp or won't display on the screen." }
            setMeasuredDimension(0, 0)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

<<<<<<< Updated upstream
=======
    private var widgetListener: WidgetListener? = null

    fun setWidgetListener(widgetListener: WidgetListener) {
        this.widgetListener = widgetListener
    }

    fun displayWidget(sdk: EngagementSDK, liveLikeWidget: LiveLikeWidget) {
        try {
            val jsonObject = GsonBuilder().create().toJson(liveLikeWidget)
            displayWidget(sdk, JsonParser.parseString(jsonObject).asJsonObject)
        } catch (ex: JsonParseException) {
            logDebug { "Invalid json passed for displayWidget" }
            ex.printStackTrace()
        }
    }

>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
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
                            engagementSDKTheme
                        )
                )
            )
=======
            if (widgetContainerViewModel?.currentWidgetViewStream?.latest() == null) {
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
                                    widgetContainerViewModel?.currentWidgetViewStream?.onNext(null)
                                },
                                sdk.userRepository,
                                null,
                                SubscriptionManager(),
                                widgetViewThemeAttributes,
                                engagementSDKTheme
                            )
                    )
                )
            } else {
                widgetContainerViewModel?.currentWidgetViewStream?.subscribe(this) {
                    if (it == null) {
                        widgetContainerViewModel?.currentWidgetViewStream?.unsubscribe(this)
                        post {
                            displayWidget(sdk, widgetResourceJson)
                        }
                    }
                }
            }
>>>>>>> Stashed changes
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
