package com.livelike.livelikesdk.widget.viewModel

import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.services.analytics.AnalyticsWidgetInteractionInfo
import com.livelike.livelikesdk.services.analytics.analyticService
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Alert
import java.lang.Exception

internal class AlertWidgetViewModel(widgetInfos: WidgetInfos, private val dismiss: ()->Unit) : WidgetViewModel(dismiss) {
    private var timeoutStarted = false
    var data: Alert? = null
    val handler : Handler

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
        currentWidgetType?.let { analyticService.trackWidgetInteraction(it, currentWidgetId, interactionData) }
    }

    private fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            analyticService.trackWidgetDismiss(
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

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            handler.postDelayed({
                dismissWidget(DismissAction.TIMEOUT)
                timeoutStarted = false
            }, AndroidResource.parseDuration(timeout))
        }
    }

    private fun cleanup() {
        timeoutStarted = false
        handler.removeCallbacksAndMessages(null)
        currentWidgetType = null
        currentWidgetId = ""
        interactionData.reset()
    }
}