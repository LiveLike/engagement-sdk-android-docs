package com.livelike.livelikesdk.widget.model

import com.livelike.livelikesdk.binding.WidgetObserver
import java.net.URI

internal class Widget {
    var observers = mutableSetOf<WidgetObserver>()
    var optionList: List<WidgetOptions> = emptyList()
    var url: URI? = null
    var id: String? = null
    var kind: String? = null
    var question: String = ""
    var optionSelected: WidgetOptions = WidgetOptions("")
    var confirmMessage: String = ""
    var correctOptionId: String = ""
    var timeout: Long = 7000L
    var subscribeChannel: String = ""
    var selectedVoteChangeUrl: String = ""

    fun optionSelectedUpdated(id: String?) {
        if (id == null) {
            optionSelected = WidgetOptions("")
            return
        }
        optionSelected = optionList.single { option -> option.id == id }
        observers.forEach { observer -> observer.optionSelectedUpdated(optionSelected.id) }
    }

    fun registerObserver(widgetObserver: WidgetObserver) {
        observers.add(widgetObserver)
    }

    private fun calculateVotePercentageForOption(option: WidgetOptions): Long {
        var voteTotal = 0L
        var answerTotal = 0L
        optionList.forEach {
            voteTotal += it.voteCount
            answerTotal += it.answerCount
        }


        return when {
            voteTotal > 0 -> (option.voteCount * 100) / voteTotal
            answerTotal > 0 -> (option.answerCount * 100) / answerTotal
            else -> 0
        }
    }

    fun notifyDataSetChange() {
        observers.forEach {
            it.questionUpdated(question)
            it.confirmMessageUpdated(confirmMessage)
            it.optionListUpdated(
                optionList.map { data ->
                    VoteOption(
                        data.id,
                        data.description ?: "",
                        calculateVotePercentageForOption(data),
                        data.imageUrl ?: "",
                        calculateVotePercentageForOption(data),
                        data.isCorrect
                    )
                },
                { optionSelectedUpdated(it) },
                Pair(correctOptionId, optionSelected.id)
            )
        }
    }
}


internal data class WidgetOptions(
    val id: String,
    val voteUrl: URI? = null,
    var description: String? = null,
    var voteCount: Long = 0,
    var imageUrl: String? = null,
    var answerCount: Long = 0,
    var answerUrl: String? = null,
    var isCorrect: Boolean = false
)


class VoteOption(
    val id: String,
    val description: String = "",
    val votePercentage: Long = 0,
    val imageUrl: String = "",
    val answerCount: Long = 0,
    val isCorrect: Boolean = false
)
