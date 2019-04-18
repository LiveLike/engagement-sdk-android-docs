package com.livelike.livelikesdk.widget.cache

import com.livelike.engagementsdkapi.WidgetStateProcessor
import com.livelike.engagementsdkapi.WidgetTransientState

class WidgetStateProcessorImpl(
    private val widgetStateMap: HashMap<String, WidgetTransientState>,
    private val currentWidgetMap: HashMap<String, String>
):
    WidgetStateProcessor {
    override var currentWidgetId: String ? = null

    override fun getWidgetState(id: String): WidgetTransientState? {
        return  widgetStateMap[id]
    }

    override fun updateWidgetState(id: String, state: WidgetTransientState) {
        if (widgetStateMap.contains(id)) {
            val currentState = widgetStateMap[id]
            if (currentState?.resultPayload ==  null)
                currentState?.resultPayload = state.resultPayload
        } else widgetStateMap[id] = state
    }

    override fun release(id: String) {
        currentWidgetId = null
        widgetStateMap.remove(id)
        currentWidgetMap.clear()
    }
}