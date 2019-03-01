package com.livelike.livelikesdk.widget.model

import com.livelike.livelikesdk.binding.Observable
import com.livelike.livelikesdk.binding.Observer
import java.net.URI
import java.util.Date
import java.util.UUID
import kotlin.properties.Delegates.observable

abstract class WidgetData : Observable {
    protected var observers = mutableSetOf<Observer>()

    override fun registerObserver(observer: Observer) { observers.add(observer) }
    override fun unRegisterObserver(observer: Observer) { observers.remove(observer) }

    abstract var optionList : List<WidgetOptionsData>

    var createdAt : Date? = null
    var url: URI ? = null
    var textPredictionId: UUID ? = null

    var question: String by observable("") { _, _, newValue ->
        observers.forEach { observer ->
            observer.questionUpdated(newValue)
        }
    }

    protected fun optionSelectedUpdated(id: UUID?) {
        optionSelected = optionList.single { option ->
            option.id == id
        }
    }

    var optionSelected: WidgetOptionsData by observable(
        WidgetOptionsData(null, null, "", 0)) { _, _, newValue ->
        observers.forEach { observer ->
            observer.optionSelectedUpdated(newValue.id!!)
        }
    }
}

data class PredictionWidgetFollowUpData(val predictionWidgetQuestionDataList: MutableList<WidgetData>) : WidgetData() {
    private val voteOptions = mutableListOf<VoteOption>()
    var correctOptionId: UUID by observable(UUID(0,0)) { _, _, _ ->
        if (!optionList.isEmpty()) {
            updateOptionList(predictionWidgetQuestionDataList)
        }
    }

    override var optionList: List<WidgetOptionsData> by observable(emptyList()) { _, _, newValue ->
        createOptionsWithVotePercentageMap(newValue)
        if (correctOptionId.leastSignificantBits != 0L && correctOptionId.mostSignificantBits != 0L) {
            updateOptionList(predictionWidgetQuestionDataList)
        }
    }

    private fun getCorrectOptionWithUserSelection(predictionWidgetDataList: MutableList<WidgetData>)
            : Pair<UUID?, UUID?> {

        var userSelectionId: UUID? = null
        // TODO: Need to add a check that questionWidget data id is equal to question widget data id.
        predictionWidgetDataList.forEach { questionWidgetData ->
            userSelectionId = questionWidgetData.optionSelected.id
        }
        return Pair(correctOptionId, userSelectionId)
    }

    private fun createOptionsWithVotePercentageMap(newValue: List<WidgetOptionsData>) {
        calculateVotePercentage(newValue)
        val optionsWithVotePercentageMap = LinkedHashMap<String, Long>()
        newValue.forEach { data ->
            optionsWithVotePercentageMap[data.description] = data.voteCount
            voteOptions.add(VoteOption(data.id, data.description, data.voteCount))
        }
    }

    private fun calculateVotePercentage(optionList: List<WidgetOptionsData>) {
        var voteTotal = 0L
        optionList.forEach { option ->
            voteTotal += option.voteCount
        }
        optionList.forEach { option ->
            if (voteTotal == 0L) return
            option.voteCount = (option.voteCount * 100) / voteTotal
        }
    }

    private fun updateOptionList(predictionWidgetDataList: MutableList<WidgetData>) {
        observers.forEach { observer ->
            observer.optionListUpdated(voteOptions,
                    { optionSelectedUpdated(it) },
                    getCorrectOptionWithUserSelection(predictionWidgetDataList))
        }
    }
}

data class WidgetOptionsData(val id: UUID?,
                             val voteUrl: URI?,
                             var description: String,
                             var voteCount: Long)


class PredictionWidgetQuestionData : WidgetData(){
    override var optionList: List<WidgetOptionsData> by observable(emptyList()) { _, _, newValue ->
        val voteOptionList = mutableListOf<VoteOption>()
        newValue.forEach { data ->
            voteOptionList.add(VoteOption(data.id, data.description, data.voteCount))
        }

        observers.forEach { observer ->
            observer.optionListUpdated(voteOptionList, { optionSelectedUpdated(it) }, Pair(null, null))
        }
    }
}

class VoteOption(val id: UUID?, val description: String, val vote: Long)
