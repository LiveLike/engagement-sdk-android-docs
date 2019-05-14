package com.livelike.livelikesdk.widget.view

import android.animation.LayoutTransition
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.view.util.SwipeDismissTouchListener

class WidgetContainerViewModel(private val widgetContainer: FrameLayout, val session: LiveLikeContentSession) {

    init {
        session.widgetTypeStream.subscribe(WidgetContainerViewModel::class.java) { widgetObserver(it) }

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
                        session.widgetTypeStream.onNext(null)
                    }
                })
        )

        // Show / Hide animation
        widgetContainer.layoutTransition = LayoutTransition()
    }

    private fun widgetObserver(widgetType: String?) {
        if (widgetType == null) {
            dismissWidget()
        } else {
            displayWidget(widgetType)
        }
    }

    private fun displayWidget(widgetView: String) {
        logDebug { "NOW - Show Widget" }
        Handler(Looper.getMainLooper()).post {
            widgetContainer.addView(
                WidgetViewProvider().get(
                    WidgetType.fromString(widgetView),
                    widgetContainer.context
                )
            )
        }
    }

    private fun dismissWidget() {
        logDebug { "NOW - Dismiss Widget" }
        widgetContainer.removeAllViews()
    }

    fun close() {
        session.widgetTypeStream.unsubscribe(WidgetContainerViewModel::class.java)
    }
}
