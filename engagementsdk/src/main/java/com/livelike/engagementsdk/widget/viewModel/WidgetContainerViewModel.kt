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
import com.livelike.engagementsdk.utils.logDebug
import com.livelike.engagementsdk.utils.logError
import com.livelike.engagementsdk.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.util.SwipeDismissTouchListener

// TODO remove view references from this view model, also clean content session for same.

class WidgetContainerViewModel(private val currentWidgetViewStream: Stream<Pair<String, SpecifiedWidgetView?>?>) {

    private var dismissWidget: ((action: DismissAction) -> Unit)? = null
    private var widgetContainer: FrameLayout? = null
    var analyticsService: AnalyticsService? = null

    @SuppressLint("ClickableViewAccessibility")
    fun setWidgetContainer(widgetContainer: FrameLayout) {
        this.widgetContainer = widgetContainer
        // Swipe to dismiss
        widgetContainer.setOnTouchListener(
            SwipeDismissTouchListener(
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
        )

        currentWidgetViewStream.subscribe(WidgetContainerViewModel::class.java) { pair ->
            widgetObserver(pair?.second, pair?.first)
        }

        // Show / Hide animation
        widgetContainer.layoutTransition = LayoutTransition()
    }

    private fun widgetObserver(widgetView: SpecifiedWidgetView?, widgetType: String?) {
        removeViews()
        if (widgetView != null) {
            displayWidget(widgetView)
        }
        if (widgetContainer != null) {
            widgetView?.widgetId?.let { widgetId ->
                analyticsService?.trackWidgetDisplayed(
                    WidgetType.fromString(
                        widgetType ?: ""
                    )?.toAnalyticsString() ?: "", widgetId
                )
            }
        }
    }

    private fun displayWidget(view: SpecifiedWidgetView?) {
        if (view != null) {
            dismissWidget = view.dismissFunc
            (view.parent as ViewGroup?)?.removeAllViews() // Clean the view parent in case of reuse
            widgetContainer?.addView(view)
            logDebug { "NOW - Show WidgetInfos" }
        } else {
            logError { "Can't display view of this type" }
        }
    }

    private fun removeViews() {
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
