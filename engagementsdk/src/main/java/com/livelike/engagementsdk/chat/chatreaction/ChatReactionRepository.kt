package com.livelike.engagementsdk.chat.chatreaction

import android.content.Context
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.BuildConfig
import com.livelike.engagementsdk.data.repository.BaseRepository
import com.livelike.engagementsdk.services.network.RequestType
import com.livelike.engagementsdk.services.network.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random

internal class ChatReactionRepository(val programId: String) : BaseRepository() {

    private val remoteUrl = BuildConfig.CONFIG_URL + "reaction-packs/?program_id=$programId"

    var reactionList: List<Reaction>? = null

    suspend fun getReactions(): List<Reaction>? {
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
            return@withContext reactionList
        }
    }

    suspend fun preloadImages(context: Context) {
        withContext(Dispatchers.IO) {
            getReactions()?.forEach {
                Glide.with(context).load(it.file).preload()
                it.reactionsCount=Random().nextInt(100000)
            }
        }
    }
}
