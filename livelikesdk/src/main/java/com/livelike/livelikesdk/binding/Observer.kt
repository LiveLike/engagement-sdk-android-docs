package com.livelike.livelikesdk.binding

import java.util.*
import kotlin.collections.LinkedHashMap

interface Observer {
    fun questionUpdated(questionText: String)
    fun optionListUpdated(
        idDescriptionVoteMap: LinkedHashMap<UUID?, Pair<String, Long>>,
        optionSelectedCallback: (UUID?) -> Unit,
        correctOptionWithUserSelection: Pair<UUID?, UUID?>)
    fun optionSelectedUpdated(selectedOptionId: UUID)
}