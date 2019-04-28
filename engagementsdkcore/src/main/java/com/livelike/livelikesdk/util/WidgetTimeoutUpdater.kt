package com.livelike.livelikesdk.util

import com.livelike.engagementsdkapi.WidgetTransientState

internal class WidgetTimeoutUpdater(private val updateRate: Int,
                                    val progressedState: WidgetTransientState,
                                    val progressedStateCallback: (WidgetTransientState) -> Unit) {

    var initialTimeout = 0L
    fun updateResultPhaseTimeout(interactionPhaseTimeout: Long,
                                 resultPhaseTimeout: Long,
                                 timeoutCompleted: () -> Unit) {
        progressedState.resultPhaseTimeout = resultPhaseTimeout - initialTimeout
        progressedState.interactionPhaseTimeout = interactionPhaseTimeout
        progressedStateCallback.invoke(progressedState)
        initialTimeout += updateRate
        if (resultPhaseTimeout == initialTimeout)
            timeoutCompleted.invoke()
    }

    fun updateInteractionPhaseTimeout(interactionPhaseTimeout: Long,
                                      resultPhaseTimeout: Long,
                                      timeoutCompleted: () -> Unit) {
        progressedState.interactionPhaseTimeout = interactionPhaseTimeout - initialTimeout
        progressedState.resultPhaseTimeout = resultPhaseTimeout
        progressedStateCallback.invoke(progressedState)
        initialTimeout += updateRate
        if (interactionPhaseTimeout == initialTimeout) {
            timeoutCompleted.invoke()
        }
    }
}