package com.livelike.engagementsdk.widget

import android.content.res.Resources
import android.graphics.Color

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

data class ImageSliderTheme(
    val body: Component? = null,
    val dismiss: Component? = null,
    val footer: Component? = null,
    val header: Component? = null,
    val marker: Component? = null,
    val timer: Component? = null,
    val title: Component? = null,
    val track: Component? = null
) : BaseTheme() {
    override fun validate(): String? {
        return body?.validate() ?: dismiss?.validate() ?: footer?.validate()
        ?: header?.validate() ?: marker?.validate() ?: timer?.validate() ?: timer?.validate()
        ?: track?.validate()
    }
}

data class CheerMeterTheme(
    val body: Component? = null,
    val dismiss: Component? = null,
    val footer: Component? = null,
    val header: Component? = null,
    val sideABar: Component? = null,
    val sideAButton: Component? = null,
    val sideBBar: Component? = null,
    val sideBButton: Component? = null,
    val timer: Component? = null,
    val title: Component? = null,
    val versus: Component? = null
) : BaseTheme() {
    override fun validate(): String? {
        return body?.validate() ?: dismiss?.validate() ?: footer?.validate() ?: header?.validate()
        ?: sideABar?.validate() ?: sideAButton?.validate() ?: sideBBar?.validate()
        ?: sideAButton?.validate() ?: timer?.validate() ?: title?.validate() ?: versus?.validate()
    }
}

data class WidgetsTheme(
    val alert: LayoutComponent? = null,
    val cheerMeter: CheerMeterTheme? = null,
    val imagePoll: LayoutPickerComponent? = null,
    val imagePrediction: LayoutPickerComponent? = null,
    val imagePredictionFollowUp: LayoutPickerComponent? = null,
    val imageQuiz: LayoutPickerComponent? = null,
    val imageSlider: ImageSliderTheme? = null,
    val textPoll: LayoutPickerComponent? = null,
    val textPrediction: LayoutPickerComponent? = null,
    val textPredictionFollowUp: LayoutPickerComponent? = null,
    val textQuiz: LayoutPickerComponent? = null
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

data class LayoutComponent(
    val body: Component? = null,
    val dismiss: Component? = null,
    val footer: Component? = null,
    val header: Component? = null,
    val timer: Component? = null,
    val title: Component? = null
) : BaseTheme() {
    override fun validate(): String? {
        return body?.validate() ?: dismiss?.validate() ?: footer?.validate() ?: header?.validate()
        ?: timer?.validate() ?: title?.validate()
    }
}

data class LayoutPickerComponent(
    val body: Component? = null,
    val correctOption: Component? = null,
    val correctOptionBar: Component? = null,
    val correctOptionDescription: Component? = null,
    val correctOptionImage: Component? = null,
    val correctOptionPercentage: Component? = null,
    val dismiss: Component? = null,
    val footer: Component? = null,
    val header: Component? = null,
    val incorrectOption: Component? = null,
    val incorrectOptionBar: Component? = null,
    val incorrectOptionDescription: Component? = null,
    val incorrectOptionImage: Component? = null,
    val incorrectOptionPercentage: Component? = null,
    val selectedOption: Component? = null,
    val selectedOptionBar: Component? = null,
    val selectedOptionDescription: Component? = null,
    val selectedOptionImage: Component? = null,
    val selectedOptionPercentage: Component? = null,
    val timer: Component? = null,
    val title: Component? = null,
    val unselectedOption: Component? = null,
    val unselectedOptionBar: Component? = null,
    val unselectedOptionDescription: Component? = null,
    val unselectedOptionImage: Component? = null,
    val unselectedOptionPercentage: Component? = null
) : BaseTheme() {
    override fun validate(): String? {
        return body?.validate() ?: correctOption?.validate() ?: correctOptionBar?.validate()
        ?: correctOptionDescription?.validate() ?: correctOptionImage?.validate()
        ?: correctOptionPercentage?.validate() ?: dismiss?.validate() ?: footer?.validate()
        ?: header?.validate()
        ?: incorrectOption?.validate() ?: incorrectOptionBar?.validate()
        ?: incorrectOptionDescription?.validate() ?: incorrectOptionImage?.validate()
        ?: incorrectOptionPercentage?.validate() ?: selectedOption?.validate()
        ?: selectedOptionBar?.validate() ?: selectedOptionDescription?.validate()
        ?: selectedOptionImage?.validate() ?: selectedOptionPercentage?.validate()
        ?: timer?.validate() ?: title?.validate() ?: unselectedOption?.validate()
        ?: unselectedOptionBar?.validate() ?: unselectedOptionDescription?.validate()
        ?: unselectedOptionImage?.validate() ?: unselectedOptionPercentage?.validate()
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