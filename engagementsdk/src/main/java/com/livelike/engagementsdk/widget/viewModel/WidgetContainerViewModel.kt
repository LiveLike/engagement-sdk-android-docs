package com.livelike.engagementsdk.widget.viewModel

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.util.SwipeDismissTouchListener
import com.livelike.engagementsdk.widget.utils.toAnalyticsString

// TODO remove view references from this view model, also clean content session for same.

class WidgetContainerViewModel(val currentWidgetViewStream: Stream<Pair<String, SpecifiedWidgetView?>?>) {

    var enableDefaultWidgetTransition: Boolean = true
        set(value) {
            field = value
            if(value){
                widgetContainer?.setOnTouchListener(
                    swipeDismissTouchListener
                )
            }else{
                widgetContainer?.setOnTouchListener(null)
            }
        }
    var widgetLifeCycleEventsListener: WidgetLifeCycleEventsListener? = null
    private lateinit var widgetViewThemeAttributes: WidgetViewThemeAttributes
    private var dismissWidget: ((action: DismissAction) -> Unit)? = null
    private var widgetContainer: FrameLayout? = null
    var analyticsService: AnalyticsService? = null

    // Swipe to dismiss
    var swipeDismissTouchListener: View.OnTouchListener? = null

    @SuppressLint("ClickableViewAccessibility")
    fun setWidgetContainer(
        widgetContainer: FrameLayout,
        widgetViewThemeAttributes: WidgetViewThemeAttributes
    ) {
        this.widgetContainer = widgetContainer
        this.widgetViewThemeAttributes = widgetViewThemeAttributes
        swipeDismissTouchListener = SwipeDismissTouchListener(
            widgetContainer,
            null,
            object : SwipeDismissTouchListener.DismissCallbacks {
                override fun canDismiss(token: Any?): Boolean {
                    return true
                }

                override fun onDismiss(view: View?, token: Any?) {
                    dismissWidget?.invoke(DismissAction.SWIPE)
                    dismissWidget = null
                    removeViews()
                }
            })
        if (enableDefaultWidgetTransition) {
            widgetContainer.setOnTouchListener(
                swipeDismissTouchListener
            )
        }
        currentWidgetViewStream.subscribe(WidgetContainerViewModel::class.java) { pair ->
            if (pair != null)
                widgetObserver(pair?.second, pair?.first)
        }
        // Show / Hide animation
        widgetContainer.layoutTransition = LayoutTransition()
    }

    private fun widgetObserver(widgetView: SpecifiedWidgetView?, widgetType: String?) {
        removeViews()
        if (widgetView != null) {
            widgetView.widgetViewModel?.enableDefaultWidgetTransition =
                enableDefaultWidgetTransition
            displayWidget(widgetView)
        }
        if (widgetContainer != null) {
            widgetView?.widgetId?.let { widgetId ->
                analyticsService?.trackWidgetDisplayed(
                    WidgetType.fromString(
                        widgetType ?: ""
                    )?.toAnalyticsString() ?: "", widgetId ,
                    widgetView?.widgetInfos?.payload?.get("link_url").asString
                )
            }
        }
    }

    private fun displayWidget(view: SpecifiedWidgetView?) {
        if (view != null) {
            dismissWidget = view.dismissFunc
            (view.parent as ViewGroup?)?.removeAllViews() // Clean the view parent in case of reuse
            view.widgetViewThemeAttributes.apply {
                widgetWinAnimation = widgetViewThemeAttributes.widgetWinAnimation
                widgetLoseAnimation = widgetViewThemeAttributes.widgetLoseAnimation
                widgetDrawAnimation = widgetViewThemeAttributes.widgetDrawAnimation
            }
            view.widgetLifeCycleEventsListener = widgetLifeCycleEventsListener
            widgetContainer?.addView(view)
            logDebug { "NOW - Show WidgetInfos" }
        } else {
            logError { "Can't display view of this type" }
        }
    }

    internal fun removeViews() {
        logDebug { "NOW - Dismiss WidgetInfos" }
        widgetContainer?.removeAllViews()
        widgetContainer?.apply {
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    !isInLayout
                } else {
                    true
                }
            ) requestLayout()
        }
    }
}
