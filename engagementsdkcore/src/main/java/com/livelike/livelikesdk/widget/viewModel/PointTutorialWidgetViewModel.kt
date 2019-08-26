package com.livelike.livelikesdk.widget.viewModel

import com.livelike.engagementsdkapi.AnalyticsService
import com.livelike.engagementsdkapi.DismissAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PointTutorialWidgetViewModel(
    private val onDismiss: () -> Unit,
    val analyticsService: AnalyticsService
) : WidgetViewModel() {
    private var timeoutStarted = false

    internal fun dismissWidget(action: DismissAction) {
        onDismiss()
        cleanup()
        analyticsService.trackPointTutorialSeen(action.name, 5000L)
        viewModelJob.cancel()
    }

    fun startDismissTimout(timeout: Long, function: () -> Unit) {
        if (!timeoutStarted) {
            timeoutStarted = true
            uiScope.launch {
                delay(timeout)
                dismissWidget(DismissAction.TIMEOUT)
                onDismiss()
                function()
                timeoutStarted = false
            }
        }
    }

    private fun cleanup() {
        timeoutStarted = false
    }
}
