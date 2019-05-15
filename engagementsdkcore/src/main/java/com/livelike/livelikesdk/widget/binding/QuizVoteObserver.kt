package com.livelike.livelikesdk.widget.binding

import com.livelike.livelikesdk.widget.model.VoteOption

internal interface QuizVoteObserver {
    fun updateVoteCount(voteOptions: List<VoteOption>)
}