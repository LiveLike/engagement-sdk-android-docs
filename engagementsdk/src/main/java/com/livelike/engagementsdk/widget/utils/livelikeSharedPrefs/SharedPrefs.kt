package com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs

import com.livelike.engagementsdk.core.data.models.NumberPredictionVotes
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.liveLikeSharedPrefs.getSharedPreferences

private const val PREFERENCE_KEY_POINTS_TUTORIAL = "PointsTutorial"
private const val PREFERENCE_KEY_POINTS_TOTAL = "PointsTotal"
private const val PREFERENCE_KEY_WIDGETS_PREDICTIONS_VOTED = "predictions-voted"
private const val PREFERENCE_KEY_WIDGETS_NUMBER_PREDICTIONS_VOTED = "number-predictions-voted"

internal fun shouldShowPointTutorial(): Boolean {
    return getSharedPreferences()
        .getBoolean(PREFERENCE_KEY_POINTS_TUTORIAL, true)
}

internal fun pointTutorialSeen() {
    if (shouldShowPointTutorial()) {
        val editor = getSharedPreferences()
            .edit()
        editor.putBoolean(PREFERENCE_KEY_POINTS_TUTORIAL, false).apply()
    }
}

internal fun addWidgetPredictionVoted(id: String, optionId: String) {
    val editor = getSharedPreferences()
        .edit()
    val idsList = getWidgetPredictionVoted().toMutableSet()
    idsList.remove(idsList.find { savedWidgetVote -> savedWidgetVote.id == id })
    idsList.add(SavedWidgetVote(id, optionId))
    editor.putString(PREFERENCE_KEY_WIDGETS_PREDICTIONS_VOTED, gson.toJson(idsList.toTypedArray()))
        .apply()
}

internal fun getWidgetPredictionVoted(): Array<SavedWidgetVote> {
    val predictionVotedJson = getSharedPreferences()
        .getString(
            PREFERENCE_KEY_WIDGETS_PREDICTIONS_VOTED, ""
        ) ?: ""
    return gson.fromJson(predictionVotedJson, Array<SavedWidgetVote>::class.java) ?: emptyArray()
}

internal fun getWidgetPredictionVotedAnswerIdOrEmpty(id: String?): String {
    return getWidgetPredictionVoted().find { it.id == id }?.optionId ?: ""
}


// number prediction widget voted saved state
internal fun addWidgetNumberPredictionVoted(id: String, widgetVoteList: List<NumberPredictionVotes>) {
    val editor = getSharedPreferences().edit()
    val idsList = getWidgetNumberPredictionVoted().toMutableSet()
    for (item in widgetVoteList) {
        idsList.add(NumberPredictionSavedWidgetVote(id, item.optionId, item.number))
    }
    editor.putString(
        PREFERENCE_KEY_WIDGETS_NUMBER_PREDICTIONS_VOTED, gson.toJson(idsList.toTypedArray()))
        .apply()
}


internal fun getWidgetNumberPredictionVoted(): Array<NumberPredictionSavedWidgetVote> {
    val predictionVotedJson = getSharedPreferences()
        .getString(
            PREFERENCE_KEY_WIDGETS_NUMBER_PREDICTIONS_VOTED, ""
        ) ?: ""
    return gson.fromJson(predictionVotedJson, Array<NumberPredictionSavedWidgetVote>::class.java)
        ?: emptyArray()
}


internal fun getWidgetNumberPredictionVotedAnswerList(id: String?): List<NumberPredictionVotes> {
    val votedList = getWidgetNumberPredictionVoted()
    if(votedList.isNotEmpty()){
        val optionList : MutableList<NumberPredictionVotes> = mutableListOf()
        for (item in votedList) {
            if (item.id == id) optionList.add(NumberPredictionVotes(item.optionId, item.number))
        }
        return optionList.toList()
    }
    return emptyList()
}


internal data class SavedWidgetVote(
    val id: String,
    val optionId: String
)

internal data class NumberPredictionSavedWidgetVote(
    val id: String,
    val optionId: String,
    val number: Int
)

internal fun getTotalPoints(): Int {
    return getSharedPreferences()
        .getInt(PREFERENCE_KEY_POINTS_TOTAL, 0)
}

internal fun addPoints(points: Int) {
    val editor = getSharedPreferences()
        .edit()
    editor.putInt(PREFERENCE_KEY_POINTS_TOTAL, points + getTotalPoints()).apply()
}
