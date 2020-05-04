package com.livelike.engagementsdk.widget

import android.content.res.Resources
import android.graphics.Color
import com.livelike.engagementsdk.widget.view.themes.AlertTheme
import com.livelike.engagementsdk.widget.view.themes.CheerMeterTheme
import com.livelike.engagementsdk.widget.view.themes.ImagePollTheme
import com.livelike.engagementsdk.widget.view.themes.ImagePredictionFollowUpTheme
import com.livelike.engagementsdk.widget.view.themes.ImagePredictionTheme
import com.livelike.engagementsdk.widget.view.themes.ImageQuizTheme
import com.livelike.engagementsdk.widget.view.themes.ImageSliderTheme
import com.livelike.engagementsdk.widget.view.themes.TextPollTheme
import com.livelike.engagementsdk.widget.view.themes.TextPredictionFollowUpTheme
import com.livelike.engagementsdk.widget.view.themes.TextPredictionTheme
import com.livelike.engagementsdk.widget.view.themes.TextQuizTheme

abstract class BaseTheme {
    abstract fun validate(): String?

    fun getColorFromString(color: String?): Int? {
        try {
            var checkedColor = color
            if (checkedColor?.contains("#") == false) {
                checkedColor = "#$checkedColor"
            }
            return Color.parseColor(checkedColor)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun pxToDp(px: Int): Int {
        return (px / Resources.getSystem().displayMetrics.density).toInt()
    }
}

data class WidgetsTheme(
    val alert: AlertTheme? = null,
    val cheerMeter: CheerMeterTheme? = null,
    val imagePoll: ImagePollTheme? = null,
    val imagePrediction: ImagePredictionTheme? = null,
    val imagePredictionFollowUp: ImagePredictionFollowUpTheme? = null,
    val imageQuiz: ImageQuizTheme? = null,
    val imageSlider: ImageSliderTheme? = null,
    val textPoll: TextPollTheme? = null,
    val textPrediction: TextPredictionTheme? = null,
    val textPredictionFollowUp: TextPredictionFollowUpTheme? = null,
    val textQuiz: TextQuizTheme? = null
) : BaseTheme() {
    override fun validate(): String? {
        return alert?.validate() ?: cheerMeter?.validate() ?: imagePoll?.validate()
        ?: imagePrediction?.validate() ?: imagePredictionFollowUp?.validate()
        ?: imageQuiz?.validate() ?: imageSlider?.validate() ?: textPoll?.validate()
        ?: textPrediction?.validate() ?: textPredictionFollowUp?.validate() ?: textQuiz?.validate()
    }
}

data class Component(
    val background: BackgroundProperty? = null,
    val borderColor: String? = null,
    val borderRadius: List<Double>? = null,
    val borderWidth: Double? = null,
    val fontColor: String? = null,
    val fontFamily: List<String>? = null,
    val fontSize: Double? = null,
    val fontWeight: FontWeight? = null,
    val margin: List<Double>? = null,
    val padding: List<Double>? = null
) : BaseTheme() {
    override fun validate(): String? {
        if (getColorFromString(borderColor) == null)
            return "Unable to parse Border Color"
        if (getColorFromString(fontColor) == null)
            return "Unable to parse Font Color"
        return background?.validate()
    }
}

data class BackgroundProperty(
    val color: String? = null,
    val format: Format,
    val colors: List<String>? = null,
    val direction: Double? = null
) : BaseTheme() {
    override fun validate(): String? {
        val colorCheck = getColorFromString(color)
        var colorsCheck: String? = null
        colors?.let {
            for (color in it) {
                if (getColorFromString(color) == null) {
                    colorsCheck = "Unable to parse Colors"
                    break
                }
            }
        }
        if (colorCheck == null && colors.isNullOrEmpty()) {
            return "Unable to parse Background Color"
        } else if (colors.isNullOrEmpty().not()) {
            return colorsCheck
        }
        return null
    }
}

enum class Format {
    Fill,
    UniformGradient
}

enum class FontWeight {
    Bold,
    Light,
    Normal
}