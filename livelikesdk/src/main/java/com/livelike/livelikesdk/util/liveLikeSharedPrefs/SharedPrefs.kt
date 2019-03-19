package com.livelike.livelikesdk.util.liveLikeSharedPrefs

import android.content.Context
import android.content.SharedPreferences
import com.livelike.livelikesdk.util.gson


private val PREFERENCE_KEY_USER_ID = "UserId"
private val PREFERENCE_KEY_NICKNAME = "Username"

private val PREFERENCE_KEY_WIDGETS_PREDICTIONS_VOTED = "predictions-voted"

private var mAppContext: Context? = null

fun initLiveLikeSharedPrefs(appContext: Context) {
    mAppContext = appContext
}

private fun getSharedPreferences(): SharedPreferences {
    return mAppContext!!.getSharedPreferences("livelike-sdk", Context.MODE_PRIVATE)
}

fun setUserId(userId: String) {
    val editor = getSharedPreferences().edit()
    editor.putString(PREFERENCE_KEY_USER_ID, userId).apply()
}

fun getUserId(): String {
    return getSharedPreferences().getString(PREFERENCE_KEY_USER_ID, "") ?: ""
}

fun setNickname(nickname: String) {
    val editor = getSharedPreferences().edit()
    editor.putString(PREFERENCE_KEY_NICKNAME, nickname).apply()
}

fun getNickename(): String {
    return getSharedPreferences().getString(PREFERENCE_KEY_NICKNAME, "") ?: ""
}

fun addWidgetPredictionVoted(id: String, optionId: String) {
    val editor = getSharedPreferences().edit()
    val idsList = getWidgetPredictionVoted().toMutableSet()
    idsList.add(SavedWidgetVote(id, optionId))
    editor.putString(PREFERENCE_KEY_WIDGETS_PREDICTIONS_VOTED, gson.toJson(idsList.toTypedArray())).apply()
}

fun getWidgetPredictionVoted(): Array<SavedWidgetVote> {
    val predictionVotedJson = getSharedPreferences().getString(PREFERENCE_KEY_WIDGETS_PREDICTIONS_VOTED, "") ?: ""
    return gson.fromJson(predictionVotedJson, Array<SavedWidgetVote>::class.java) ?: emptyArray()
}

fun getWidgetPredictionVotedAnswerIdOrEmpty(id: String): String {
    return getWidgetPredictionVoted().find { it.id == id }?.optionId ?: ""
}

data class SavedWidgetVote(
    val id: String,
    val optionId: String
)