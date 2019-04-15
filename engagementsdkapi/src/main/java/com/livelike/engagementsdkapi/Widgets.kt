package com.livelike.engagementsdkapi

import com.google.gson.JsonObject

/**
 * The WidgetRenderer is in charged of:
 * - Listening for new widget messages coming from the widgetListener
 * - Restoring the widget state when the activity is destroyed
 * - Dismissing the current widget showing on screen
 * - Displaying a widget on screen
 *
 */
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

/**
 * Interface used to propagate events from the widget view to the business layer.
 * - onAnalyticsEvent: Sends analytics event upstream
 * - onWidgetEvent: TODO
 * - onWidgetDisplayed: Register on the CMS that a widget has been displayed
 * - onOptionVote: Voting for an option
 * - onOptionVoteUpdate: Updating the vote for an option
 * - onFetchingQuizResult: Requesting the quiz results to be displayed
 *
 */
interface WidgetEventListener {
    fun onAnalyticsEvent(data: Any)
    fun onWidgetEvent(event: WidgetEvent)
    fun onWidgetDisplayed(impressionUrl: String)
    fun onOptionVote(voteUrl: String, channel : String, voteUpdateCallback: ((String)-> Unit)?)
    fun onOptionVoteUpdate(oldVoteUrl:String, newVoteId:String, channel: String, voteUpdateCallback: ((String)-> Unit)?)
    fun onFetchingQuizResult(answerUrl: String)
}

/**
 * List of events the widget can handle.
 */
enum class WidgetEvent{
    WIDGET_DISMISS,
    WIDGET_SHOWN
}

/**
 * The WidgetStateProcessor is used to manage and restore widget state when the activity is being destroyed.
 *
 */
interface WidgetStateProcessor {
    var currentWidgetId: String ?
    fun getWidgetState(id: String): WidgetTransientState?
    fun updateWidgetState(id: String, state: WidgetTransientState)
    fun release(id: String)
}

/**
 * A saved instance of a widget.
 * This data is used by the WidgetStateProcessor to restore the widget in his previous state.
 *
 */
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