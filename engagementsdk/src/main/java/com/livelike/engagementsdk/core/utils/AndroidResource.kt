package com.livelike.engagementsdk.core.utils

import android.content.Context
import android.content.res.Resources
import android.os.Build
import com.google.gson.Gson
import org.threeten.bp.Duration
import org.threeten.bp.format.DateTimeParseException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Random

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

        inline fun <reified T> getFileFromAsset(context: Context, path: String): T? {
            try {
                val asset = context.assets
                val br = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    BufferedReader(InputStreamReader(asset.open(path), StandardCharsets.UTF_8))
                } else {
                    BufferedReader(InputStreamReader(asset.open(path)))
                }
                val sb = StringBuilder()
                var str: String?
                while (br.readLine().also { str = it } != null) {
                    sb.append(str)
                }
                br.close()
                val gson = Gson()
                return gson.fromJson(sb.toString(), T::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
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
