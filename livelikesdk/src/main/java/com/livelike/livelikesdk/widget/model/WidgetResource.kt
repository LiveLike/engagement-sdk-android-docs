package com.livelike.livelikesdk.widget.model

import java.io.Serializable

data class Resource(
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
    val text_prediction_id: String = "",
    val text_prediction_url: String = "",
    val correct_option_id: String = "",
    val confirmation_message: String = "",
    val testTag: String = "",
    val choices: List<Option> = listOf(),
    val options: List<Option> = listOf()
) : Serializable {
    val totalVotes: Int by lazy {
        options.map { it.vote_count }.sum()
        }
    val totalAnswers: Int by lazy {
        choices.map { it.answer_count }.sum()
    }
}

data class Alert(
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
    val link_label: String = ""
)

data class Option(
    val id: String,
    val url: String = "",
    val description: String = "",
    val is_correct: Boolean = false,
    val answer_url: String = "",
    val vote_url: String = "",
    val image_url: String = "",
    val answer_count: Int = 0,
    val vote_count: Int = 0
) {
    fun getPercentVote(total: Float): Int {
        if (total == 0F) return 0
        return Math.round((vote_count / total) * 100)
    }

    fun getPercentAnswer(total: Float): Int {
        if (total == 0F) return 0
        return Math.round((answer_count / total) * 100)
    }
}

data class Vote(
    val id: String,
    val url: String = "",
    val choice_id: String = "",
    val option_id: String = ""
)