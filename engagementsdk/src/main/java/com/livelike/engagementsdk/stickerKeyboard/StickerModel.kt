package com.livelike.engagementsdk.stickerKeyboard

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Sticker(val file : String, val shortcode : String) : Parcelable
data class StickerPack(val name : String, val file: String, val stickers : List<Sticker>)
data class StickerPackResults(val results: List<StickerPack>)
