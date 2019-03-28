package com.livelike.engagementsdkapi

import com.google.gson.JsonObject


interface WidgetRenderer {
    var widgetListener: WidgetEventListener?

    fun dismissCurrentWidget()
    fun displayWidget(
        type: String,
        payload: JsonObject
    )
}

interface WidgetEventListener {
    fun onAnalyticsEvent(data: Any)
    fun onWidgetEvent(event: WidgetEvent)
    fun onOptionVote(voteUrl: String, subscribeChannel: String)
    fun onWidgetDisplayed(impressionUrl: String)
    fun onFetchingQuizResult(answerUrl: String)
}

enum class WidgetEvent{
    WIDGET_DISMISS,
    WIDGET_SHOWN
}
