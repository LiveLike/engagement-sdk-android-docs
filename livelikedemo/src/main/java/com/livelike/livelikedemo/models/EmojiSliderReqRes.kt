package com.livelike.livelikedemo.models

import com.livelike.livelikedemo.CreatedBy
import com.livelike.livelikedemo.Option

data class EmojiSliderRequest(
    val initial_magnitude: Double? = null,
    val options: List<Option>? = null,
    val program_date_time: String? = null,
    val program_id: String? = null,
    val question: String? = null,
    val timeout: String? = null
)