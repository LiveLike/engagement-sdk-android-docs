package com.livelike.engagementsdk.stickerKeyboard

import android.content.Context
import com.livelike.engagementsdk.utils.SubscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StickerKeyboardViewModel(private val stickerPackRepository: StickerPackRepository){
    internal var stickerPacks = SubscriptionManager<List<StickerPack>>()

    init {
        GlobalScope.launch {
            val stickers = stickerPackRepository.getStickerPacks()
            withContext(Dispatchers.Main){
                stickerPacks.onNext(stickers)
            }
        }
    }

    fun getFromShortcode(s:String) : Sticker?{
        return stickerPackRepository.getSticker(s)
    }

    fun preload(c: Context){
        stickerPackRepository.preloadImages(c)
    }
}