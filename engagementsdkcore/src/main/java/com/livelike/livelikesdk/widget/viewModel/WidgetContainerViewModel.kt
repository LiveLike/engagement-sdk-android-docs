package com.livelike.livelikesdk.widget.viewModel

import android.animation.LayoutTransition
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.utils.logDebug
import com.livelike.livelikesdk.utils.logError
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.WidgetViewProvider
import com.livelike.livelikesdk.widget.util.SwipeDismissTouchListener

class WidgetContainerViewModel(private val widgetContainer: FrameLayout, val session: LiveLikeContentSession) {

    init {
        session.widgetStream.subscribe(WidgetContainerViewModel::class.java) { s: String?, j: JsonObject? ->
            widgetObserver(
                s,
                j
            )
        }

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
                        session.widgetStream.onNext(null, null)
                    }
                })
        )

        // Show / Hide animation
        widgetContainer.layoutTransition = LayoutTransition()
    }

    private fun widgetObserver(type: String?, payload: JsonObject?) {
        if (type.isNullOrEmpty()) {
            dismissWidget()
        } else {
            displayWidget(type)
        }
    }

    private fun displayWidget(widgetView: String) {

        Handler(Looper.getMainLooper()).post {
            val view = WidgetViewProvider().get(
                WidgetType.fromString(widgetView),
                widgetContainer.context
            )
            if (view != null) {
                widgetContainer.addView(view)
                logDebug { "NOW - Show Widget" }
            } else {
                logError { "Can't display view of type: $widgetView" }
            }
        }
    }

    private fun dismissWidget() {
        logDebug { "NOW - Dismiss Widget" }
        widgetContainer.removeAllViews()
    }

    fun close() {
        session.widgetStream.unsubscribe(WidgetContainerViewModel::class.java)
    }
}
