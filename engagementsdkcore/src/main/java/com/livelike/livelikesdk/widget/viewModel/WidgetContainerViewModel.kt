package com.livelike.livelikesdk.widget.viewModel

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.Stream
import com.livelike.livelikesdk.utils.logDebug
import com.livelike.livelikesdk.utils.logError
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import com.livelike.livelikesdk.widget.util.SwipeDismissTouchListener

class WidgetContainerViewModel(private val currentWidgetViewStream: Stream<SpecifiedWidgetView?>) {

    private var dismissWidget: ((action: DismissAction) -> Unit)? = null
    private var widgetContainer: FrameLayout? = null

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
                        dismissWidget()
                    }
                })
        )

        currentWidgetViewStream.subscribe(WidgetContainerViewModel::class.java) { widgetView: SpecifiedWidgetView? ->
            widgetObserver(widgetView)
        }

        // Show / Hide animation
        widgetContainer.layoutTransition = LayoutTransition()
    }

    private fun widgetObserver(widgetView: SpecifiedWidgetView?) {
        dismissWidget()
        if (widgetView != null) {
            displayWidget(widgetView)
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

    private fun dismissWidget() {
        logDebug { "NOW - Dismiss WidgetInfos" }
        widgetContainer?.removeAllViews()

    }
}
