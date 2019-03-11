package com.livelike.livelikesdk.widget.model

import com.livelike.livelikesdk.binding.Observable
import com.livelike.livelikesdk.binding.WidgetObserver
import java.net.URI
import java.util.Date
import kotlin.collections.LinkedHashMap
import kotlin.collections.set
import kotlin.properties.Delegates.observable

abstract class Widget : Observable {
    protected var observers = mutableSetOf<WidgetObserver>()

    override fun registerObserver(widgetObserver: WidgetObserver) { observers.add(widgetObserver) }
    override fun unRegisterObserver(widgetObserver: WidgetObserver) { observers.remove(widgetObserver) }

    abstract var optionList : List<WidgetOptions>

    var createdAt : Date? = null
    var url: URI ? = null
    var id: String ? = null

    var question: String by observable("") { _, _, newValue ->
        observers.forEach { observer ->
            observer.questionUpdated(newValue)
        }
    }

    protected fun optionSelectedUpdated(id: String?) {
        if (id == null) {
            optionSelected = WidgetOptions(null, null, "", 0    )
            return
        }
        optionSelected = optionList.single { option ->
            option.id == id
        }
    }

    var optionSelected: WidgetOptions by observable(
        WidgetOptions(null, null, "", 0)) { _, _, newValue ->
        observers.forEach { observer ->
            observer.optionSelectedUpdated(newValue.id)
        }
    }

    var confirmMessage: String by observable("") { _, _, newValue ->
        observers.forEach { observer ->
            observer.confirmMessageUpdated(newValue)
        }
    }
}

class PredictionWidgetFollowUp : Widget() {
    private val voteOptions = mutableListOf<VoteOption>()
    var questionWidgetId : String = ""
    var correctOptionId: String by observable("") { _, _, _ ->
        if (!optionList.isEmpty()) {
            updateOptionList()
        }
    }

    override var optionList: List<WidgetOptions> by observable(emptyList()) { _, _, newValue ->
        createOptionsWithVotePercentageMap(newValue)
        if (correctOptionId != "") {
            updateOptionList()
        }
    }

    private fun getCorrectOptionWithUserSelection()
            : Pair<String?, String?> {
        return Pair(correctOptionId, optionSelected.id)
    }

    private fun createOptionsWithVotePercentageMap(newValue: List<WidgetOptions>) {
        calculateVotePercentage(newValue)
        val optionsWithVotePercentageMap = LinkedHashMap<String, Long>()
        newValue.forEach { data ->
            optionsWithVotePercentageMap[data.description] = data.voteCount
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

    private fun updateOptionList() {
        observers.forEach { observer ->
            observer.optionListUpdated(voteOptions,
                    { optionSelectedUpdated(it) },
                    getCorrectOptionWithUserSelection())
        }
    }
}

data class WidgetOptions(val id: String? = null,
                         val voteUrl: URI? = null,
                         var description: String = "",
                         var voteCount: Long = 0) {
    var imageUrl: String? = null
}


class PredictionWidgetQuestion : Widget(){
    override var optionList: List<WidgetOptions> by observable(emptyList()) { _, _, newValue ->
        val voteOptionList = mutableListOf<VoteOption>()
        newValue.forEach { data ->
            voteOptionList.add(VoteOption(data.id, data.description, data.voteCount, data.imageUrl))
        }

        observers.forEach { observer ->
            observer.optionListUpdated(voteOptionList, { optionSelectedUpdated(it) }, Pair(null, null))
        }
    }
}

class VoteOption(val id: String?, val description: String, val votePercentage: Long, val imageUrl: String?)
