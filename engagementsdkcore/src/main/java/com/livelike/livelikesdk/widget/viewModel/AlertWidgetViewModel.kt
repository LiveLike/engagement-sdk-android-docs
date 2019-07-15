package com.livelike.livelikesdk.widget.viewModel

import android.os.Handler
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.services.analytics.AnalyticsService
import com.livelike.livelikesdk.services.analytics.AnalyticsWidgetInteractionInfo
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Alert

internal class AlertWidgetViewModel(widgetInfos: WidgetInfos, private val dismiss: () -> Unit, private val analyticsService: AnalyticsService) : WidgetViewModel(dismiss) {
    private var timeoutStarted = false
    var data: Alert? = null
    val handler: Handler

    private var currentWidgetId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()

    init {
        data = gson.fromJson(widgetInfos.payload.toString(), Alert::class.java) ?: null
        interactionData.widgetDisplayed()
        currentWidgetId = widgetInfos.widgetId
        currentWidgetType = WidgetType.fromString(widgetInfos.type)
        handler = Handler()
    }

    fun onClickLink() {
        interactionData.incrementInteraction()
        currentWidgetType?.let { analyticsService.trackWidgetInteraction(it, currentWidgetId, interactionData) }
    }

    private fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            analyticsService.trackWidgetDismiss(
                it,
                currentWidgetId,
                interactionData,
                false,
                action
            )
        }
        cleanup()
        dismiss()
    }

    private val runnable = Runnable {
        dismissWidget(DismissAction.TIMEOUT)
        timeoutStarted = false
    }

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, AndroidResource.parseDuration(timeout))
        }
    }

    private fun cleanup() {
        timeoutStarted = false
        handler.removeCallbacks(runnable)
        handler.removeCallbacksAndMessages(null)
        currentWidgetType = null
        currentWidgetId = ""
        interactionData.reset()
    }
}