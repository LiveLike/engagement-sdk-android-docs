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
import com.livelike.engagementsdk.FontFamilyProvider
import com.livelike.engagementsdk.widget.FontWeight
import com.livelike.engagementsdk.widget.Format
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

        fun webPxToDevicePx(px: Int): Int {
            val scale = Resources.getSystem().displayMetrics.density
            return ((px * 0.6F) * scale + 0.5f).toInt()
        }

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

        fun updateThemeForView(
            textView: TextView,
            component: ViewStyleProps?,
            fontFamilyProvider: FontFamilyProvider? = null
        ) {
            component?.let {
                textView.apply {
                    it.fontSize?.let {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, it.toFloat())
                    }
                    it.fontColor?.let {
                        setTextColor(getColorFromString(it) ?: Color.WHITE)
                    }

                    if (fontFamilyProvider != null) {
                        it.fontFamily?.forEach {
                            val typeFace = fontFamilyProvider.getTypeFace(it)
                            if (typeFace != null) {
                                typeface = typeFace
                                return@forEach
                            }
                        }
                    }
                    it.fontWeight?.let {
                        setTypeface(
                            typeface, when (it) {
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
                webPxToDevicePx(padding?.get(0)?.toInt() ?: 0),
                webPxToDevicePx(padding?.get(1)?.toInt() ?: 0),
                webPxToDevicePx(padding?.get(2)?.toInt() ?: 0),
                webPxToDevicePx(padding?.get(3)?.toInt() ?: 0)
            )
        }

        fun createDrawable(
            component: ViewStyleProps?
        ): GradientDrawable? {
            var shape: GradientDrawable = GradientDrawable()
            component?.background?.let {
                if (it.format == Format.Fill.key) {
                    if (it.color!!.isNotEmpty())
                        shape.setColor(getColorFromString(it.color) ?: Color.TRANSPARENT)
                } else {
                    if (it.colors.isNullOrEmpty().not())
                        shape.colors =
                            it.colors?.map { c -> getColorFromString(c) ?: 0 }?.toIntArray()
                    else
                        shape.colors = null
                }
            }

            shape.orientation =
                component?.background?.direction?.toInt()
                    ?.let { selectGradientDirection(it) }

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

        internal fun selectGradientDirection(direction: Int): GradientDrawable.Orientation {
            return GradientDrawable.Orientation.LEFT_RIGHT
            // commenting as direction to be implemented later
//            return when (direction) {
//                0 -> GradientDrawable.Orientation.BOTTOM_TOP
//                45 -> GradientDrawable.Orientation.TOP_BOTTOM
//                90 -> GradientDrawable.Orientation.BL_TR
//                135 -> GradientDrawable.Orientation.BR_TL
//                180 -> GradientDrawable.Orientation.LEFT_RIGHT
//                225 -> GradientDrawable.Orientation.RIGHT_LEFT
//                270 -> GradientDrawable.Orientation.TL_BR
//                else -> {
//                    GradientDrawable.Orientation.TR_BL
//                }
//            }
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
