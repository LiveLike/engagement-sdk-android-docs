package com.livelike.engagementsdk.widget.model

internal data class Resource(
    val id: String = "",
    val url: String = "",
    val kind: String = "",
    val question: String = "",
    val timeout: String = "",
    val subscribe_channel: String = "",
    val program_id: String = "",
    val created_at: String = "",
    val published_at: String = "",
    val follow_up_url: String = "",
    val rewards_url: String?,
    val text_prediction_id: String = "",
    val image_prediction_id: String = "",
    val text_prediction_url: String = "",
    val correct_option_id: String = "",
    val confirmation_message: String = "",
    val testTag: String = "",
    val choices: List<Option>? = listOf(),
    val options: List<Option>? = listOf(),
    val impression_url: String = ""
) {
    fun getMergedOptions(): List<Option>? {
        return if (choices != null && choices.isNotEmpty()) {
            choices
        } else {
            options
        }
    }

    fun getMergedTotal(): Int {
        val totalAnswers = options?.map { it.vote_count ?: 0 }?.sum() ?: 0
        return if (totalAnswers == 0) {
            choices?.map { it.answer_count ?: 0 }?.sum() ?: 0
        } else {
            totalAnswers
        }
    }

    init {
        getMergedOptions()?.forEach {
            it.percentage = it.getPercent(getMergedTotal().toFloat())
        }
    }
}

internal data class Alert(
    val id: String = "",
    val url: String = "",
    val kind: String = "",
    val timeout: String = "",
    val subscribe_channel: String = "",
    val program_id: String = "",
    val created_at: String = "",
    val published_at: String = "",
    val title: String = "",
    val text: String = "",
    val image_url: String = "",
    val link_url: String = "",
    val link_label: String = "",
    val impression_url: String = ""
)

internal data class Option(
    val id: String,
    val url: String = "",
    val description: String = "",
    val is_correct: Boolean = false,
    val answer_url: String? = "",
    val vote_url: String? = "",
    val image_url: String? = "",
    var answer_count: Int? = 0,
    var vote_count: Int? = 0
) {
    @Deprecated("Use getPercent instead")
    fun getPercentVote(total: Float): Int {
        if (total == 0F) return 0
        return Math.round((vote_count!! / total) * 100)
    }

    fun getPercent(total: Float): Int {
        if (total == 0F) return 0
        val nbVote: Int = answer_count ?: (vote_count ?: 0)
        return Math.round((nbVote / total) * 100)
    }

    fun getMergedVoteCount(): Float {
        return (answer_count ?: (vote_count ?: 0)).toFloat()
    }

    fun getMergedVoteUrl(): String? {
        return if (vote_url.isNullOrEmpty()) {
            answer_url
        } else {
            vote_url
        }
    }

    var percentage = 0

    fun updateCount(option: Option) {
        answer_count = option.answer_count
        vote_count = option.vote_count
    }
}

internal data class Vote(
    val id: String,
    val url: String = "",
    val choice_id: String = "",
    val option_id: String = ""
)

internal data class Reward(
    val id: String?,
    val rank: Int?,
    val new_points: Int?,
    val points: Int?
)