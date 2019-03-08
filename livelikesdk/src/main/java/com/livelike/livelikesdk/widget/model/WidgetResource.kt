package com.livelike.livelikesdk.widget.model

import java.io.Serializable

data class Resource(
    var id: String = "",
    var url: String = "",
    var kind: String = "",
    var question: String = "",
    val timeout: String = "",
    var subscribe_channel: String = "",
    var program_id: String = "",
    var created_at: String = "",
    var published_at: String = "",
    var follow_up_url: String = "",
    var text_prediction_id: String = "",
    var text_prediction_url: String = "",
    var correct_option_id: String = "",
    var confirmation_message: String = "",
    var tweets: List<TweetItem>,
    var choices: List<Option> = listOf(),
    var options: List<Option> = listOf()
) : Serializable {
    val totalVotes: Float
        get() {
            var tempValue = 0F
            for (it in options) {
                tempValue += it.vote_count
            }
            return tempValue
        }
    val totalAnswers: Float
        get() {
            var tempValue = 0F
            for (it in choices) {
                tempValue += it.answer_count
            }
            return tempValue
        }
}

data class Alert(
    var id: String = "",
    var url: String = "",
    var kind: String = "",
    val timeout: String = "",
    var subscribe_channel: String = "",
    var program_id: String = "",
    var created_at: String = "",
    var published_at: String = "",
    var title: String = "",
    var text: String = "",
    var image_url: String = "",
    var link_url: String = "",
    var link_label: String = ""
) : Serializable

data class Option(
    val id: String,
    var url: String = "",
    var description: String = "",
    var is_correct: Boolean = false,
    var answer_url: String = "",
    var vote_url: String = "",
    var image_url: String = "",
    var state: String = "default",
    var answer_count: Int = 0,
    var vote_count: Int = 0
) : Serializable {
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
    var url: String = "",
    var choice_id: String = "",
    var option_id: String = ""
) : Serializable

data class TweetItem(
    val tweet_id: Long
) : Serializable