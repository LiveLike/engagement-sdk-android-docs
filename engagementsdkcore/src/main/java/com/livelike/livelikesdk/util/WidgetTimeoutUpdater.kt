package com.livelike.livelikesdk.util

import com.livelike.engagementsdkapi.WidgetTransientState

internal class WidgetTimeoutUpdater(private val updateRate: Int,
                                    val progressedState: WidgetTransientState,
                                    val progressedStateCallback: (WidgetTransientState) -> Unit) {

    var initialTimeout = 0L
    fun updateResultPhaseTimeout(interactionPhaseTimeout: Long,
                                 resultPhaseTimeout: Long,
                                 completeTimeoutValue: Long,
                                 timeoutCompleted: () -> Unit) {
        progressedState.resultPhaseTimeout = resultPhaseTimeout
        progressedState.interactionPhaseTimeout = interactionPhaseTimeout
        progressedStateCallback.invoke(progressedState)
        initialTimeout += updateRate
        if (completeTimeoutValue == initialTimeout)
            timeoutCompleted.invoke()
    }
}