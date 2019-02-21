package com.livelike.livelikesdk.binding

import com.livelike.livelikesdk.widget.model.WidgetOptionsData

interface Observer {
    fun questionUpdated(questionText: String)
    fun optionListUpdated(optionList: Map<String, Long>,
                          optionSelectedCallback: (CharSequence?) -> Unit,
                          correctOptionWithUserSelection: Pair<String?, String?>)
    fun optionSelectedUpdated(selectedOption: WidgetOptionsData)
}