package com.livelike.livelikesdk.widget.view

import android.animation.LayoutTransition
import android.arch.lifecycle.ViewModelProviders
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.view.util.SwipeDismissTouchListener

class WidgetPresenter(private val widgetContainer: FrameLayout, val session: LiveLikeContentSession) {
    // This is the only way I found to save data between the session being dismissed
    private val widgetViewModel: WidgetViewModel =
        ViewModelProviders.of(widgetContainer.context as AppCompatActivity).get(WidgetViewModel::class.java)

    init {
        session.widgetContext = widgetContainer.context
        session.widgetTypeStream.subscribe(WidgetPresenter::class.java) { widgetObserver(it) }
        widgetObserver(widgetViewModel.currentWidget)

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
        widgetViewModel.currentWidget = widgetType
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
        session.widgetTypeStream.unsubscribe(WidgetPresenter::class.java)
        widgetViewModel.currentWidget = null
    }
}
