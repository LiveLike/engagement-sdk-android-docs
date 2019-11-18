package com.livelike.engagementsdk.utils.liveLikeSharedPrefs

import android.content.Context
import android.content.SharedPreferences
import com.livelike.engagementsdk.stickerKeyboard.Sticker
import com.livelike.engagementsdk.utils.gson

private const val PREFERENCE_KEY_SESSION_ID = "SessionId"
private const val PREFERENCE_KEY_NICKNAME = "Username"
private const val PREFERENCE_KEY_USER_PIC = "Userpic"
private const val PREFERENCE_KEY_POINTS_TUTORIAL = "PointsTutorial"
private const val PREFERENCE_KEY_POINTS_TOTAL = "PointsTotal"
private const val PREFERENCE_KEY_WIDGETS_PREDICTIONS_VOTED = "predictions-voted"
private const val BLOCKED_USERS = "blocked-users"
private const val RECENT_STICKERS = "recent-stickers"
private const val RECENT_STICKERS_DELIMITER = "~~~~"
internal const val PREFERENCE_CHAT_ROOM_MEMBERSHIP = "chat-room-membership"
private var mAppContext: Context? = null

internal fun initLiveLikeSharedPrefs(appContext: Context) {
    mAppContext = appContext
}

internal fun getSharedPreferences(): SharedPreferences {
    return mAppContext!!.getSharedPreferences("livelike-sdk", Context.MODE_PRIVATE)
}

internal fun getSessionId(): String {
    return getSharedPreferences().getString(PREFERENCE_KEY_SESSION_ID, "") ?: ""
}

internal fun setNickname(nickname: String) {
    val editor = getSharedPreferences().edit()
    editor.putString(PREFERENCE_KEY_NICKNAME, nickname).apply()
}

internal fun getNickename(): String {
    return getSharedPreferences().getString(PREFERENCE_KEY_USER_PIC, "") ?: ""
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

internal fun blockUser(userId: String) {
    val editor = getSharedPreferences().edit()
    val currentList = getSharedPreferences().getString(BLOCKED_USERS, "") ?: ""
    if (!currentList.contains(userId)) {
        editor.putString(BLOCKED_USERS, "$currentList,$userId").apply()
    }
}

internal fun getBlockedUsers(): List<String> {
    val currentList = getSharedPreferences().getString(BLOCKED_USERS, "") ?: ""
    return currentList.split(",")
}

internal fun shouldShowPointTutorial(): Boolean {
    return getSharedPreferences().getBoolean(PREFERENCE_KEY_POINTS_TUTORIAL, true)
}

internal fun pointTutorialSeen() {
    if (shouldShowPointTutorial()) {
        val editor = getSharedPreferences().edit()
        editor.putBoolean(PREFERENCE_KEY_POINTS_TUTORIAL, false).apply()
    }
}

internal fun addRecentSticker(sticker: Sticker) {
    val editor = getSharedPreferences().edit()
    val stickerSet: MutableSet<String> = HashSet(getSharedPreferences().getStringSet(RECENT_STICKERS + sticker.programId, setOf()) ?: setOf()).toMutableSet() // The data must be copied to a new array, see doc https://developer.android.com/reference/android/content/SharedPreferences.html#getStringSet(java.lang.String,%20java.util.Set%3Cjava.lang.String%3E)
    stickerSet.add(sticker.file + RECENT_STICKERS_DELIMITER + sticker.shortcode)
    editor.putStringSet(RECENT_STICKERS + sticker.programId, stickerSet)?.apply()
}

internal fun getRecentStickers(programId: String): List<Sticker> {
    val stickerSet: Set<String> = getSharedPreferences().getStringSet(RECENT_STICKERS + programId, setOf()) ?: setOf()
    return stickerSet.map { Sticker(it.split(RECENT_STICKERS_DELIMITER)[0], it.split(RECENT_STICKERS_DELIMITER)[1]) }
}
