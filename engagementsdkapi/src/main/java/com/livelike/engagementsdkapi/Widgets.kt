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
    fun onWidgetDisplayed(impressionUrl: String)
    fun onOptionVote(voteUrl: String, channel : String, voteUpdateCallback: ((String)-> Unit)?)
    fun onOptionVoteUpdate(oldVoteUrl:String, newVoteId:String , channel: String, voteUpdateCallback: ((String)-> Unit)?)
    fun onFetchingQuizResult(answerUrl: String)
}

enum class WidgetEvent{
    WIDGET_DISMISS,
    WIDGET_SHOWN
}
