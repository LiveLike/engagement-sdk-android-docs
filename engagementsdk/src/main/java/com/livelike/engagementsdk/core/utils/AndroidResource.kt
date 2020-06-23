package com.livelike.engagementsdk.core.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.google.gson.Gson
import com.livelike.engagementsdk.widget.FontWeight
import com.livelike.engagementsdk.widget.ViewStyleProps
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
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

        fun getColorFromString(color: String?): Int? {
            try {
                if (color.isNullOrEmpty().not()) {
                    var checkedColor = color
                    if (checkedColor?.contains("#") == false) {
                        checkedColor = "#$checkedColor"
                    }
                    return Color.parseColor(checkedColor)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun updateThemeForView(textView: TextView, component: ViewStyleProps?) {
            component?.let {
                textView.apply {
                    it.fontSize?.let {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, it.toFloat())
                    }
                    it.fontColor?.let {
                        setTextColor(getColorFromString(it) ?: Color.WHITE)
                    }
                    it.fontWeight?.let {
                        setTypeface(
                            null, when (it) {
                                FontWeight.Bold -> Typeface.BOLD
                                FontWeight.Light -> Typeface.NORMAL
                                FontWeight.Normal -> Typeface.NORMAL
                            }
                        )
                    }
                }
            }
        }

        fun setPaddingForView(view: View, padding: List<Double>?) {
            view.setPadding(
                dpToPx(padding?.get(0)?.toInt() ?: 0),
                dpToPx(padding?.get(1)?.toInt() ?: 0),
                dpToPx(padding?.get(2)?.toInt() ?: 0),
                dpToPx(padding?.get(3)?.toInt() ?: 0)
            )
        }

        fun createUpdateDrawable(
            component: ViewStyleProps?,
            shape: GradientDrawable = GradientDrawable()
        ): GradientDrawable? {
            component?.background?.let {
//                shape.shape = GradientDrawable.RECTANGLE
                if (it.colors.isNullOrEmpty().not())
                    shape.colors = it.colors?.map { c -> getColorFromString(c) ?: 0 }?.toIntArray()
                else
                    shape.colors = null
                if (it.color != null)
                    shape.setColor(getColorFromString(it.color) ?: Color.TRANSPARENT)
            }
            if (component?.borderRadius.isNullOrEmpty()
                    .not() && component?.borderRadius?.size == 4
            ) {
                shape.cornerRadii = floatArrayOf(
                    dpToPx(component.borderRadius[0].toInt()).toFloat(),
                    dpToPx(component.borderRadius[0].toInt()).toFloat(),
                    dpToPx(component.borderRadius[1].toInt()).toFloat(),
                    dpToPx(component.borderRadius[1].toInt()).toFloat(),
                    dpToPx(component.borderRadius[2].toInt()).toFloat(),
                    dpToPx(component.borderRadius[2].toInt()).toFloat(),
                    dpToPx(component.borderRadius[3].toInt()).toFloat(),
                    dpToPx(component.borderRadius[3].toInt()).toFloat()
                )
            }
            if (component?.borderColor.isNullOrEmpty()
                    .not() && component?.borderWidth != null
            ) {
                shape.setStroke(
                    dpToPx(component.borderWidth.toInt()),
                    getColorFromString(component.borderColor) ?: Color.TRANSPARENT
                )
            }
            return shape
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
