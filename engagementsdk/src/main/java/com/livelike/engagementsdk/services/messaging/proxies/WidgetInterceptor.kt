package com.livelike.engagementsdk.services.messaging.proxies

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.utils.SubscriptionManager

/**
 * Integrator will pass implementation of this proxy to intercept widgets,
 * SDK will call widgetWantsToShow() and in same sequence/order sdk will listen on events stream for the decision on that widget.
 */
abstract class WidgetInterceptor {
    abstract fun widgetWantsToShow()
    internal val events: Stream<Decision> =
        SubscriptionManager(false)

    fun showWidget() {
        events.onNext(Decision.Show)
    }

    fun dismissWidget() {
        events.onNext(Decision.Dismiss)
    }

    internal enum class Decision {
        Show,
        Dismiss
    }
}
