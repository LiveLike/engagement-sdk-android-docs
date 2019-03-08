package com.livelike.livelikesdk.analytics

interface InteractionSession{
    class InteractionType(val name : String) {
        override fun toString(): String {
            return "InteractionType $name"
        }
    }

    fun recordInteraction(interactionType: InteractionType, widgetId: String)
}