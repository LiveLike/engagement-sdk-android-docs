package com.livelike.livelikesdk.utils

import android.content.Context
import android.content.res.Resources
import java.util.Random
import org.threeten.bp.Duration
import org.threeten.bp.format.DateTimeParseException

fun Any.unit() = Unit
internal class AndroidResource {

    companion object {
        fun dpToPx(dp: Int): Int {
            val scale = Resources.getSystem().displayMetrics.density
            return (dp * scale + 0.5f).toInt()
        }

        fun pxToDp(px: Int): Int {
            val scale = Resources.getSystem().displayMetrics.density
            return ((px - 0.5f) / scale).toInt()
        }

        fun selectRandomLottieAnimation(path: String, context: Context): String? {
            val asset = context.assets
            val assetList = asset?.list(path)
            val random = Random()
            return "$path/" + if (assetList!!.isNotEmpty()) {
                val emojiIndex = random.nextInt(assetList.size)
                assetList[emojiIndex]
            } else return null
        }

        fun parseDuration(durationString: String): Long {
            var timeout = 7000L
            try {
                timeout = Duration.parse(durationString).toMillis()
            } catch (e: DateTimeParseException) {
                logError { "Duration $durationString can't be parsed." }
            }
            return timeout
        }
    }
}
