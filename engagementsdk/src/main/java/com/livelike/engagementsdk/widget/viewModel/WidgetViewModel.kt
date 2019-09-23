package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO inherit all widget viewModels from here and  add widget common code here.
internal abstract class WidgetViewModel(
    private val onDismiss: () -> Unit,
    val analyticsService: AnalyticsService
) : ViewModel() {

    private var timeoutStarted = false

    open fun dismissWidget(action: DismissAction) {
        onDismiss()
        onClear()
    }

    fun startDismissTimeout(timeout: Long, function: () -> Unit) {
        if (!timeoutStarted) {
            timeoutStarted = true
            uiScope.launch {
                delay(timeout)
                dismissWidget(DismissAction.TIMEOUT)
                function()
                timeoutStarted = false
            }
        }
    }

    open fun onClear() {
        viewModelJob.cancel()
        timeoutStarted = false
    }
}
