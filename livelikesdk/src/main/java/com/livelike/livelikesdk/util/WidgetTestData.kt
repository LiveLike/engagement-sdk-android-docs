package com.livelike.livelikesdk.util

import com.livelike.livelikesdk.widget.model.PredictionWidgetFollowUpData
import com.livelike.livelikesdk.widget.model.PredictionWidgetQuestionData
import com.livelike.livelikesdk.widget.model.WidgetData
import com.livelike.livelikesdk.widget.model.WidgetOptionsData
import java.net.URI
import java.util.UUID

class WidgetTestData {
    companion object {
        val questionWidgetDataList = mutableListOf<WidgetData>()
        private val optionFirstId = UUID.fromString("0de19009-1a2b-4924-a0d2-7518fc44a6db")
        private val optionSecondId = UUID.fromString("d49504eb-89f4-4752-9ad1-ec41dcc21c8a")
        private val optionThirdId = UUID.fromString("e49504eb-89f4-4752-9ad1-ec41dcc21c8a")
        private val voteUrl = URI.create("https://livelike-blast.herokuapp.com/")
        private const val predictionQuestion = "Who will be the man of the match?"
        private const val optionFirstDescription = "Sidney Crosby"
        private const val optionSecondDescription = "Alexander Ovechkin"
        private const val optionThirdDescription = "Parel Anthony"
        private const val optionFirstVoteCount = 211L
        private const val optionSecondVoteCount = 611L
        private const val optionThirdVoteCount = 411L
        private val optionList = listOf(
            WidgetOptionsData(
                optionFirstId,
                voteUrl,
                optionFirstDescription,
                0L
            ),
            WidgetOptionsData(
                optionSecondId,
                voteUrl,
                optionSecondDescription,
                0L
            ),
            WidgetOptionsData(
                optionThirdId,
                voteUrl,
                optionThirdDescription,
                0L
            )
        )

        fun fillDummyDataInTextPredictionQuestionData(questionWidgetData : PredictionWidgetQuestionData)
                : PredictionWidgetQuestionData {
            questionWidgetData.question = predictionQuestion
            questionWidgetData.optionList = optionList
            questionWidgetDataList.add(questionWidgetData)
            return questionWidgetData
        }

        fun fillDummyDataInTextPredictionFollowUpData(followUpWidgetData : PredictionWidgetFollowUpData)
                : PredictionWidgetFollowUpData {
            followUpWidgetData.question = predictionQuestion
            followUpWidgetData.correctOptionId = optionSecondId
            optionList.forEachIndexed{ index , data ->
                when (index) {
                    0 -> data.voteCount = optionFirstVoteCount
                    1 -> data.voteCount = optionSecondVoteCount
                    2 -> data.voteCount = optionThirdVoteCount
                }
            }
            followUpWidgetData.optionList = optionList
            return followUpWidgetData
        }
    }
}