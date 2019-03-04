package com.livelike.livelikesdk.binding

import com.livelike.livelikesdk.widget.model.VoteOption

interface WidgetObserver {
    fun questionUpdated(questionText: String)
    fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>)
    fun optionSelectedUpdated(selectedOptionId: String?)
}