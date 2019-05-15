package com.livelike.livelikesdk.utils

import com.livelike.engagementsdkapi.WidgetTransientState

internal class WidgetTimeoutUpdater(
    private val updateRate: Int,
    val progressedState: WidgetTransientState,
    val progressedStateCallback: (WidgetTransientState) -> Unit
) {

    var initialTimeout = 0L

    fun updatePhaseTimeout(
        timeout: Long,
        phase: WidgetTransientState.Phase,
        timeoutCompleted: () -> Unit
    ) {
        progressedState.phaseTimeouts[phase] = timeout - initialTimeout
        progressedStateCallback.invoke(progressedState)
        initialTimeout += updateRate
        if (timeout == initialTimeout) {
            progressedState.widgetTimeout = 0L
            timeoutCompleted.invoke()
        }
    }
}