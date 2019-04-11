package com.livelike.livelikesdk.widget.cache

import com.livelike.engagementsdkapi.WidgetStateProcessor
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.util.logInfo

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
        widgetStateMap[id] = state
    }

    override fun release(id: String) {
        currentWidgetId = null
        widgetStateMap.remove(id)
        currentWidgetMap.clear()
    }
}