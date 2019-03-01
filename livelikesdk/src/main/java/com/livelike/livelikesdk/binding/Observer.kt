package com.livelike.livelikesdk.binding

import com.livelike.livelikesdk.widget.model.VoteOption
import java.util.*

interface Observer {
    fun questionUpdated(questionText: String)
    fun optionListUpdated(
        voteOptions: MutableList<VoteOption>,
        optionSelectedCallback: (UUID?) -> Unit,
        correctOptionWithUserSelection: Pair<UUID?, UUID?>)
    fun optionSelectedUpdated(selectedOptionId: UUID)
}