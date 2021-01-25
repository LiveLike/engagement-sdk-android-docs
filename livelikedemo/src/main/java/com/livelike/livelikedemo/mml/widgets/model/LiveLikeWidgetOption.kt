package com.livelike.livelikedemo.mml.widgets.model

data class LiveLikeWidgetOption(
    val id: String,
    val description: String = "",
    val isCorrect: Boolean = false,
    val imageUrl: String? = "",
    var percentage: Int? = 0
)