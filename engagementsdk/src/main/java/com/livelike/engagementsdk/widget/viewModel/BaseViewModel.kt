package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.core.utils.SubscriptionManager

abstract class BaseViewModel : ViewModel() {

    val widgetState: Stream<WidgetStates> =
        SubscriptionManager<WidgetStates>(emitOnSubscribe = true)
}

enum class WidgetStates {
    READY,//the data has received and ready to use to inject into view
    INTERACTING,//the data is injected into view and shown
    RESULTS,// interaction completed and result to be shown
    FINISHED,//dismiss the widget
    VOTED,// user has done the voting
    EARN_REWARDS,//user earned rewards
}