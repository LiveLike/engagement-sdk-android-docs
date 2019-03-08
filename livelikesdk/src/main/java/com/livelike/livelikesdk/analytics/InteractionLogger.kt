package com.livelike.livelikesdk.analytics

import com.livelike.livelikesdk.util.logInfo

class InteractionLogger : InteractionSession {
    override fun recordInteraction(interactionType: InteractionSession.InteractionType, widgetId: String) {
        logInfo { "Interaction type ${interactionType.name} and widgetId = $widgetId" }
    }
}