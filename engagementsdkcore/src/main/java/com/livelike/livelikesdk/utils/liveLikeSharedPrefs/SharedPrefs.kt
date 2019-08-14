package com.livelike.livelikesdk.utils.liveLikeSharedPrefs

import android.content.Context
import android.content.SharedPreferences
import com.livelike.livelikesdk.utils.gson

private const val PREFERENCE_KEY_SESSION_ID = "SessionId"
private const val PREFERENCE_KEY_NICKNAME = "Username"
private const val PREFERENCE_KEY_POINTS_TUTORIAL = "PointsTutorial"
private const val PREFERENCE_KEY_WIDGETS_PREDICTIONS_VOTED = "predictions-voted"
private var mAppContext: Context? = null

internal fun initLiveLikeSharedPrefs(appContext: Context) {
    mAppContext = appContext
}

private fun getSharedPreferences(): SharedPreferences {
    return mAppContext!!.getSharedPreferences("livelike-sdk", Context.MODE_PRIVATE)
}

internal fun setSessionId(sessionId: String) {
    val editor = getSharedPreferences().edit()
    editor.putString(PREFERENCE_KEY_SESSION_ID, sessionId).apply()
}

internal fun getSessionId(): String {
    return getSharedPreferences().getString(PREFERENCE_KEY_SESSION_ID, "") ?: ""
}

internal fun setNickname(nickname: String) {
    val editor = getSharedPreferences().edit()
    editor.putString(PREFERENCE_KEY_NICKNAME, nickname).apply()
}

internal fun getNickename(): String {
    return getSharedPreferences().getString(PREFERENCE_KEY_NICKNAME, "") ?: ""
}

internal fun addWidgetPredictionVoted(id: String, optionId: String) {
    val editor = getSharedPreferences().edit()
    val idsList = getWidgetPredictionVoted().toMutableSet()
    idsList.remove(idsList.find { savedWidgetVote -> savedWidgetVote.id == id })
    idsList.add(SavedWidgetVote(id, optionId))
    editor.putString(PREFERENCE_KEY_WIDGETS_PREDICTIONS_VOTED, gson.toJson(idsList.toTypedArray())).apply()
}

internal fun getWidgetPredictionVoted(): Array<SavedWidgetVote> {
    val predictionVotedJson = getSharedPreferences().getString(PREFERENCE_KEY_WIDGETS_PREDICTIONS_VOTED, "") ?: ""
    return gson.fromJson(predictionVotedJson, Array<SavedWidgetVote>::class.java) ?: emptyArray()
}

internal fun getWidgetPredictionVotedAnswerIdOrEmpty(id: String): String {
    return getWidgetPredictionVoted().find { it.id == id }?.optionId ?: ""
}

internal data class SavedWidgetVote(
    val id: String,
    val optionId: String
)

internal fun getShouldShowPointsTutorial(): Int {
    return getSharedPreferences().getInt(PREFERENCE_KEY_POINTS_TUTORIAL, PointsTutorialState.NEVER_SHOWN.value)
}

internal fun setShouldShowPointsTutorial() {
    if (getShouldShowPointsTutorial() != PointsTutorialState.SHOWN.value) {
        val editor = getSharedPreferences().edit()
        editor.putInt(PREFERENCE_KEY_POINTS_TUTORIAL, PointsTutorialState.NEED_SHOW.value).apply()
    }
}

internal fun setShownPointsTutorial() {
    val editor = getSharedPreferences().edit()
    editor.putInt(PREFERENCE_KEY_POINTS_TUTORIAL, PointsTutorialState.SHOWN.value).apply()
}

internal enum class PointsTutorialState(val value: Int) {
    NEVER_SHOWN(0),
    NEED_SHOW(1),
    SHOWN(2),
}
