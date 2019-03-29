package com.livelike.livelikesdk.binding

import com.livelike.livelikesdk.widget.model.VoteOption

internal interface QuizVoteObserver {
    fun updateVoteCount(voteOptions: List<VoteOption>)
}