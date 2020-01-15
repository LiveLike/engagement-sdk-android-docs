package com.livelike.engagementsdk.chat.chatreaction

import android.content.Context
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.data.repository.BaseRepository
import com.livelike.engagementsdk.services.network.RequestType
import com.livelike.engagementsdk.services.network.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ChatReactionRepository private constructor(private val remoteUrl: String) :
    BaseRepository() {

    var reactionList: List<Reaction>? = null
    var reactionMap: Map<String, Reaction>? = null

    suspend fun getReactions(): List<Reaction>? {
//        return null
        return withContext(Dispatchers.IO) {
            if (reactionList == null) {
                reactionList = try {
                    val result = dataClient.remoteCall<ReactionPackResults>(
                        remoteUrl,
                        RequestType.GET,
                        accessToken = null
                    )
                    if (result is Result.Success) {
                        val reactionPack = result.data
                        reactionPack.results[0].emojis
                    } else {
                        listOf()
                    }
                } catch (e: Exception) {
                    listOf()
                }
            }
            initReactionMap(reactionList)
            return@withContext reactionList
        }
    }

    private fun initReactionMap(reactionList: List<Reaction>?) {
        reactionList?.let { list ->
            reactionMap = list.map { it.id to it }.toMap()
        }
    }

    fun getReaction(id: String): Reaction? {
        return reactionMap?.get(id)
    }

    suspend fun preloadImages(context: Context) {
        withContext(Dispatchers.IO) {
            getReactions()?.forEach {
                Glide.with(context).load(it.file).preload()
            }
        }
    }

    companion object {

        @Volatile
        private var INSTANCE: ChatReactionRepository? = null

        fun getInstance(remoteUrl: String): ChatReactionRepository =
            INSTANCE ?: ChatReactionRepository(remoteUrl).also { INSTANCE = it }
    }
}
