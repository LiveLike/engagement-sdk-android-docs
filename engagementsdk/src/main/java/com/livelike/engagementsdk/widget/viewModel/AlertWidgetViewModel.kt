package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Alert
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class AlertWidgetViewModel(
    widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    private val onDismiss: () -> Unit
) : ViewModel() {
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
        currentWidgetType?.let { analyticsService.trackWidgetInteraction(it.toAnalyticsString(), currentWidgetId, interactionData) }
    }

    internal fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            analyticsService.trackWidgetDismiss(
                it.toAnalyticsString(),
                currentWidgetId,
                interactionData,
                false,
                action
            )
        }
        onDismiss()
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
