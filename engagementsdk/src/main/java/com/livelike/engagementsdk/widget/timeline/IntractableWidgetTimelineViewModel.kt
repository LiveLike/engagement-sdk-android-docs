package com.livelike.engagementsdk.widget.timeline

import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates

/** This class is responsible for managing intractable widgets in timeline.
 * By default all widgets will be intractable without timer.
 * @contentSession: object of LiveLikeContentSession
 * predicate for filtering the widgets to only specific kind of widgets
 */
class IntractableWidgetTimelineViewModel
    (
    contentSession: LiveLikeContentSession,
    predicate: (LiveLikeWidget) -> Boolean = { _ -> true }
) :
    WidgetTimeLineViewModel(
        contentSession, predicate
    ) {

    /**
     * Decides widget interaction, if widgets will be intractable or not
     * By default all widgets will be intractable
     */
    override fun decideWidgetInteraction(
        liveLikeWidget: LiveLikeWidget,
        timeLineWidgetApiSource: WidgetApiSource
    ): WidgetStates {
        val isInteractive = if (decideWidgetInteractivity != null) {
            decideWidgetInteractivity?.wouldAllowWidgetInteraction(liveLikeWidget) ?: true
        } else {
            true
        }
        return if (isInteractive) WidgetStates.INTERACTING else WidgetStates.RESULTS
    }
}
