package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.util.logDebug

class WidgetPresenter(val widgetContainer: FrameLayout, val session: LiveLikeContentSession) {
    // This is the only way I would to save data between the session being dismissed
    var widgetViewModel: WidgetViewModel =
        ViewModelProviders.of(widgetContainer.context as AppCompatActivity).get(WidgetViewModel::class.java)

    /**
     * Sets the Session on the widget view.
     * The widget events coming from the LiveLike CMS will flow through the session
     * and display widget where this view is being drawn.
     *
     * @param session The session used on the widget view.
     */
    init {
        session.widgetStream.subscribe(WidgetPresenter::class.java) { widgetObserver(it) }
        widgetObserver(widgetViewModel.currentWidget)
    }

    private fun widgetObserver(widgetView: View?) {
        if (widgetView == null) {
            dismissWidget()
        } else {
            displayWidget(widgetView)
        }
        widgetViewModel.currentWidget = widgetView
    }

    private fun displayWidget(widgetView: View) {
        logDebug { "NOW - Show Widget" }
        widgetView.parent?.apply {
            this as ViewGroup
            removeView(widgetView)
        }
        widgetContainer.addView(widgetView)
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
