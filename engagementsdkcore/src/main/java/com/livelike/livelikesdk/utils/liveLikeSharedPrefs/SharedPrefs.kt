package com.livelike.livelikesdk.utils.liveLikeSharedPrefs

import android.content.Context
import android.content.SharedPreferences
import com.livelike.livelikesdk.utils.gson

private const val PREFERENCE_KEY_SESSION_ID = "SessionId"
private const val PREFERENCE_KEY_NICKNAME = "Username"
private const val PREFERENCE_KEY_POINTS_TUTORIAL = "PointsTutorial"
private const val PREFERENCE_KEY_POINTS_TOTAL = "PointsTotal"
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

internal fun getWidgetPredictionVotedAnswerIdOrEmpty(id: String?): String {
    return getWidgetPredictionVoted().find { it.id == id }?.optionId ?: ""
}

internal data class SavedWidgetVote(
    val id: String,
    val optionId: String
)

internal fun getTotalPoints(): Int {
    return getSharedPreferences().getInt(PREFERENCE_KEY_POINTS_TOTAL, 0)
}

internal fun addPoints(points: Int) {
    val editor = getSharedPreferences().edit()
    editor.putInt(PREFERENCE_KEY_POINTS_TOTAL, points + getTotalPoints()).apply()
}

internal fun shouldShowPointTutorial(): Boolean {
    return getSharedPreferences().getBoolean(PREFERENCE_KEY_POINTS_TUTORIAL, true)
}

internal fun pointTutorialSeen() {
    val editor = getSharedPreferences().edit()
    editor.putBoolean(PREFERENCE_KEY_POINTS_TUTORIAL, false).apply()
}
