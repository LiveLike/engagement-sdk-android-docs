package com.livelike.livelikesdk.util

import com.livelike.livelikesdk.widget.model.PredictionWidgetFollowUp
import com.livelike.livelikesdk.widget.model.PredictionWidgetQuestion
import com.livelike.livelikesdk.widget.model.Widget
import com.livelike.livelikesdk.widget.model.WidgetOptions
import java.net.URI

class WidgetTestData {
    companion object {
        val questionWidgetDataList = mutableListOf<Widget>()
        private const val optionFirstId = "0de19009-1a2b-4924-a0d2-7518fc44a6db"
        private const val optionSecondId = "d49504eb-89f4-4752-9ad1-ec41dcc21c8a"
        private const val optionThirdId = "e49504eb-89f4-4752-9ad1-ec41dcc21c8a"
        private val voteUrl = URI.create("https://livelike-blast.herokuapp.com/")
        private const val predictionQuestion = "Who will be the man of the match?"
        private const val optionFirstDescription = "Sidney Crosby"
        private const val optionSecondDescription = "Alexander Ovechkin"
        private const val optionThirdDescription = "Parel Anthony"
        private const val optionFirstVoteCount = 211L
        private const val optionSecondVoteCount = 611L
        private const val optionThirdVoteCount = 411L
        private val optionList = listOf(
            WidgetOptions(
                optionFirstId,
                voteUrl,
                optionFirstDescription,
                0L,
                null
            ),
            WidgetOptions(
                optionSecondId,
                voteUrl,
                optionSecondDescription,
                0L,
                null
            ),
            WidgetOptions(
                optionThirdId,
                voteUrl,
                optionThirdDescription,
                0L,
                null
            )
        )

        fun fillDummyDataInTextPredictionQuestionData(questionWidgetData : PredictionWidgetQuestion)
                : PredictionWidgetQuestion {
            questionWidgetData.question = predictionQuestion
            questionWidgetData.optionList = optionList
            questionWidgetDataList.add(questionWidgetData)
            return questionWidgetData
        }

        fun fillDummyDataInTextPredictionFollowUpData(followUpWidgetData : PredictionWidgetFollowUp)
                : PredictionWidgetFollowUp {
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