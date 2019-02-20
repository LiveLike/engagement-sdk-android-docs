package com.livelike.livelikesdk.widget.model

import android.util.Log
import com.livelike.livelikesdk.binding.Observable
import com.livelike.livelikesdk.widget.Observer
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

    protected fun optionSelectedUpdated(description: String) {
        optionSelected = optionList.single { option ->
            option.description == description
        }
    }

    var optionSelected: WidgetOptionsData by observable(
        WidgetOptionsData(null, null, "", 0)) { _, _, newValue ->
        observers.forEach { observer ->
            observer.optionSelectedUpdated(newValue)
        }
    }
}

data class FollowupWidgetData(val predictionWidgetDataList: MutableList<WidgetData>) : WidgetData() {
    private var  descriptionList =  LinkedHashMap<String, Long>()
    var correctOptionId: UUID by observable(UUID(0,0)) { _, _, _ ->
        if (!optionList.isEmpty()) {
            updateOptionList(descriptionList, predictionWidgetDataList)
        }
    }

    override var optionList: List<WidgetOptionsData> by observable(emptyList()) { _, _, newValue ->
        val descriptionList = createOptionsWithVotePercentageMap(newValue)
        if (correctOptionId.leastSignificantBits != 0L && correctOptionId.mostSignificantBits != 0L) {
            Log.v("Main", "Abhishek descriptionList")
            updateOptionList(descriptionList, predictionWidgetDataList)
        }
    }

    private fun getCorrectOptionWithUserSelection(predictionWidgetDataList: MutableList<WidgetData>)
            : Pair<String?, String?> {

        var userSelection: String? = null
        var correctOption : String? = null
        // TODO: Need to add a check that questionWidget data id is equal to question widget data id.
        predictionWidgetDataList.forEach { questionWidgetData ->
            userSelection = questionWidgetData.optionSelected.description
        }
        optionList.forEach { option ->
            if (option.id == correctOptionId)
                correctOption = option.description
        }
        return Pair(correctOption, userSelection)
    }

    private fun createOptionsWithVotePercentageMap(newValue: List<WidgetOptionsData>): LinkedHashMap<String, Long> {
        calculateVotePercentage(newValue)
        val optionsWithVotePercentageMap = LinkedHashMap<String, Long>()
        newValue.forEach { data ->
            optionsWithVotePercentageMap[data.description] = data.voteCount
        }
        return optionsWithVotePercentageMap
    }

    private fun calculateVotePercentage(optionList: List<WidgetOptionsData>) {
        var voteTotal = 0L
        optionList.forEach { option ->
            voteTotal += option.voteCount
        }
        optionList.forEach { option ->
            option.voteCount = (option.voteCount * 100) / voteTotal
        }
    }

    private fun updateOptionList(descriptionList: LinkedHashMap<String, Long>,
                                 predictionWidgetDataList: MutableList<WidgetData>) {
        observers.forEach { observer ->
            observer.optionListUpdated(descriptionList,
                    { optionSelectedUpdated(it.toString()) },
                    getCorrectOptionWithUserSelection(predictionWidgetDataList))
        }
    }
}

data class WidgetOptionsData(val id: UUID?,
                             val voteUrl: URI?,
                             var description: String,
                             var voteCount: Long)


class PredictionWidgetData : WidgetData(){
    override var optionList: List<WidgetOptionsData> by observable(emptyList()) { _, _, newValue ->
        val descriptionList = LinkedHashMap<String, Long>()
        newValue.forEach { data ->
            descriptionList[data.description] = data.voteCount
        }
        observers.forEach { observer ->
            observer.optionListUpdated(descriptionList, { optionSelectedUpdated(it.toString()) }, Pair(null, null))
        }
    }
}
