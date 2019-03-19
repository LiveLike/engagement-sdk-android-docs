package com.livelike.livelikesdk.util

import android.content.Context
import android.content.res.Resources
import java.util.*

internal class AndroidResource {
    companion object {
        fun dpToPx(dp: Int): Int {
            val scale = Resources.getSystem().displayMetrics.density
            return (dp * scale + 0.5f).toInt()
        }

        fun selectRandomLottieAnimation(path: String, context: Context): String? {
            val asset = context.assets
            val assetList = asset?.list(path)
            val random = Random()
            return if (assetList!!.isNotEmpty()) {
                val emojiIndex = random.nextInt(assetList.size)
                assetList[emojiIndex]
            } else return null
        }
    }

}