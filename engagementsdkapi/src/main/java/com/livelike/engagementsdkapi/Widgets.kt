package com.livelike.engagementsdkapi

import com.google.gson.JsonObject


interface WidgetRenderer {
    var widgetListener: WidgetEventListener?
    var widgetStateProcessor: WidgetStateProcessor?
    fun dismissCurrentWidget()
    fun displayWidget(
        type: String,
        payload: JsonObject,
        startingState: WidgetTransientState
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

interface WidgetStateProcessor {
    var currentWidgetId: String ?
    fun getWidgetState(id: String): WidgetTransientState?
    fun updateWidgetState(id: String, state: WidgetTransientState)
    fun release(id: String)
}


class WidgetTransientState {
    var timeout = 0L
    var userSelection: String? = null
    var timerAnimatorStartPhase = 0f
    var type: String? = null
    var payload: JsonObject? = null
    var resultAnimationPath: String? = null
    var resultAnimatorStartPhase = 0f
    var timeStamp = 0L
}