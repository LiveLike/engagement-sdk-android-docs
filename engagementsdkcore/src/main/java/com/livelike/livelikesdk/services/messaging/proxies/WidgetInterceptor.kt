package com.livelike.livelikesdk.services.messaging.proxies

import com.livelike.livelikesdk.Stream
import com.livelike.livelikesdk.utils.SubscriptionManager

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
