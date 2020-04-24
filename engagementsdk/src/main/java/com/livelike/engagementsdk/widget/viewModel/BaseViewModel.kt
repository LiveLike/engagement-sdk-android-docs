package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.core.utils.SubscriptionManager

abstract class BaseViewModel : ViewModel() {

    internal val widgetState: Stream<WidgetStates> =
        SubscriptionManager<WidgetStates>(emitOnSubscribe = true)
    internal var enableDefaultWidgetTransition = true

}

enum class WidgetStates {
    READY,//the data has received and ready to use to inject into view
    INTERACTING,//the data is injected into view and shown
    RESULTS,// interaction completed and result to be shown
    FINISHED,//dismiss the widget
}