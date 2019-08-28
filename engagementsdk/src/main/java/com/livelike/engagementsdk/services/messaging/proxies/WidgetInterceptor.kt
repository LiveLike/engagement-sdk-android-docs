package com.livelike.engagementsdk.services.messaging.proxies

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.utils.SubscriptionManager

abstract class WidgetInterceptor {
    abstract fun widgetWantsToShow()
    val events: Stream<Decision> =
        SubscriptionManager()
    fun showWidget() {
        events.onNext(Decision.Show)
    }
    fun dismissWidget() {
        events.onNext(Decision.Dismiss)
    }
    enum class Decision {
        Show,
        Dismiss
    }
}
