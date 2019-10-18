package com.livelike.engagementsdk.stickerKeyboard

import android.content.Context
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.livelike.engagementsdk.BuildConfig
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StickerPackRepository(programId: String) {
    private val endpoint = BuildConfig.CONFIG_URL + "sticker-packs/?program_id=$programId"
    private var stickerPackList: List<StickerPack>? = null

    suspend fun getStickerPacks(): List<StickerPack> {
        return withContext(Dispatchers.IO) {
            if (stickerPackList == null) {
                stickerPackList = try {
                    val response = URL(endpoint).readText()
                    val stickerRes = Gson().fromJson(response, StickerPackResults::class.java)
                    stickerRes.results
                } catch (e: Exception) {
                    listOf()
                }
            }
            return@withContext stickerPackList as List<StickerPack>
        }
    }

    fun preloadImages(context: Context) {
        stickerPackList?.forEach {
            Glide.with(context).load(it.file).preload()
            it.stickers.forEach { sticker ->
                Glide.with(context).load(sticker.file).preload()
            }
        }
    }

    // When in the input the user type :shortcode: a regex captures it and this fun is used to find the corresponding sticker if it exists.
    fun getSticker(shortcode: String): Sticker? {
        return stickerPackList?.map { it.stickers }?.flatten()?.find { it.shortcode == shortcode }
    }
}
