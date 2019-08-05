package com.livelike.livelikesdk.widget.viewModel

import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.services.analytics.AnalyticsService
import com.livelike.livelikesdk.services.analytics.AnalyticsWidgetInteractionInfo
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Alert
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class AlertWidgetViewModel(widgetInfos: WidgetInfos, private val analyticsService: AnalyticsService) : WidgetViewModel() {
    private var timeoutStarted = false
    var data: SubscriptionManager<Alert?> = SubscriptionManager()

    private var currentWidgetId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()

    init {
        data.onNext(gson.fromJson(widgetInfos.payload.toString(), Alert::class.java) ?: null)
        interactionData.widgetDisplayed()
        currentWidgetId = widgetInfos.widgetId
        currentWidgetType = WidgetType.fromString(widgetInfos.type)
    }

    fun onClickLink() {
        interactionData.incrementInteraction()
        currentWidgetType?.let { analyticsService.trackWidgetInteraction(it, currentWidgetId, interactionData) }
    }

    internal fun dismissWidget(action: DismissAction) {
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
        viewModelJob.cancel()
    }

    fun startDismissTimout(timeout: String, onDismiss: () -> Unit) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                dismissWidget(DismissAction.TIMEOUT)
                onDismiss()
                timeoutStarted = false
            }
        }
    }

    private fun cleanup() {
        data.onNext(null)
        timeoutStarted = false
        currentWidgetType = null
        currentWidgetId = ""
        interactionData.reset()
    }
}
