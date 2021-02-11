package com.livelike.engagementsdk.widget.model

import com.livelike.engagementsdk.LiveLikeWidget

class LiveLikeWidgetResponse(
    val list: List<LiveLikeWidget>?,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)