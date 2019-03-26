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
    var optionSelected: WidgetOptions = WidgetOptions()
    var confirmMessage: String = ""
    var correctOptionId: String = ""
    var timeout: Long = 7000L

    fun optionSelectedUpdated(id: String?) {
        if (id == null) {
            optionSelected = WidgetOptions()
            return
        }
        optionSelected = optionList.single { option -> option.id == id }
        observers.forEach { observer -> observer.optionSelectedUpdated(optionSelected.id) }
    }

    fun registerObserver(widgetObserver: WidgetObserver) {
        observers.add(widgetObserver)
    }
}

internal class PredictionWidgetFollowUp(val widget: Widget) {
    private val voteOptions = mutableListOf<VoteOption>()
    private fun getCorrectOptionWithUserSelection()
            : Pair<String?, String?> {
        return Pair(widget.correctOptionId, widget.optionSelected.id)
    }

    private fun createOptionsWithVotePercentageMap(newValue: List<WidgetOptions>) {
        calculateVotePercentage(newValue)
        newValue.forEach { data ->
            voteOptions.add(VoteOption(data.id, data.description, data.voteCount, data.imageUrl))
        }
    }

    private fun calculateVotePercentage(optionList: List<WidgetOptions>) {
        var voteTotal = 0L
        optionList.forEach { option ->
            voteTotal += option.voteCount
        }
        optionList.forEach { option ->
            if (voteTotal == 0L) return
            option.voteCount = (option.voteCount * 100) / voteTotal
        }
    }

    fun notifyDataSetChange() {
        createOptionsWithVotePercentageMap(widget.optionList)
        widget.observers.forEach { observer ->
            observer.questionUpdated(widget.question)
            observer.confirmMessageUpdated(widget.confirmMessage)
            observer.optionListUpdated(
                voteOptions,
                { widget.optionSelectedUpdated(it) },
                getCorrectOptionWithUserSelection()
            )
        }
    }
}

internal data class WidgetOptions(
    val id: String? = null,
    val voteUrl: URI? = null,
    var description: String = "",
    var voteCount: Long = 0,
    var imageUrl: String? = null
)

internal class PredictionWidgetQuestion(val widget: Widget) {
    val id = widget.id
    fun notifyDataSetChange() {
        val voteOptionList = mutableListOf<VoteOption>()
        widget.optionList.forEach { data ->
            voteOptionList.add(VoteOption(data.id, data.description, data.voteCount, data.imageUrl))
        }
        widget.observers.forEach { observer ->
            observer.questionUpdated(widget.question)
            observer.confirmMessageUpdated(widget.confirmMessage)
            observer.optionListUpdated(voteOptionList, { widget.optionSelectedUpdated(it) }, Pair(null, null))
        }
    }
}

class VoteOption(val id: String?, val description: String, val votePercentage: Long, val imageUrl: String?)
