package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.WidgetType

class WidgetPresenter(private val widgetContainer: FrameLayout, val session: LiveLikeContentSession) {
    // This is the only way I would to save data between the session being dismissed
    private val widgetViewModel: WidgetViewModel =
        ViewModelProviders.of(widgetContainer.context as AppCompatActivity).get(WidgetViewModel::class.java)

    init {
        session.widgetStream.subscribe(WidgetPresenter::class.java) { widgetObserver(it) }
        widgetObserver(widgetViewModel.currentWidget)
    }

    private fun widgetObserver(widgetView: String?) {
        if (widgetView == null) {
            dismissWidget()
        } else {
            displayWidget(widgetView)
        }
        widgetViewModel.currentWidget = widgetView
    }

    private fun displayWidget(widgetView: String) {
        logDebug { "NOW - Show Widget" }
        widgetContainer.addView(WidgetViewProvider().get(WidgetType.fromString(widgetView), widgetContainer.context))
    }

    private fun dismissWidget() {
        logDebug { "NOW - Dismiss Widget" }
        widgetContainer.removeAllViews()
    }

    fun close() {
        session.widgetStream.unsubscribe(WidgetPresenter::class.java)
        widgetViewModel.currentWidget = null
    }
}
