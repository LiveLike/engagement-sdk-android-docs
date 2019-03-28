package com.livelike.livelikesdk.binding

internal interface QuizWidgetObserver : WidgetObserver {
    fun updateVoteCount(vote: Long)
}