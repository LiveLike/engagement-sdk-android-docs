package com.livelike.engagementsdk.services.messaging.proxies

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.utils.SubscriptionManager

/**
 * Integrator will pass implementation of this proxy to intercept widgets,
 * SDK will call widgetWantsToShow() and in same sequence/order sdk will listen on events stream for the decision on that widget.
 */
abstract class WidgetInterceptor {
    /** Called when a widget is received from the CMS */
    abstract fun widgetWantsToShow()

    /** Unlock the widget and show it on screen */
    fun showWidget() {
        events.onNext(Decision.Show)
    }

    /** Dismiss the widget and won't show it on screen */
    fun dismissWidget() {
        events.onNext(Decision.Dismiss)
    }

    internal enum class Decision {
        Show,
        Dismiss
    }

    internal val events: Stream<Decision> =
        SubscriptionManager(false)

}
