package com.livelike.engagementsdk.widget

import com.livelike.engagementsdk.core.utils.AndroidResource

abstract class BaseTheme {
    abstract fun validate(): String?
}

abstract class WidgetBaseThemeComponent(
    val body: ViewStyleProps? = null,
    val dismiss: ViewStyleProps? = null,
    val footer: ViewStyleProps? = null,
    val header: ViewStyleProps? = null,
    val timer: ViewStyleProps? = null,
    val title: ViewStyleProps? = null
) : BaseTheme() {

    override fun validate(): String? {
        return body?.validate() ?: dismiss?.validate() ?: footer?.validate()
        ?: header?.validate() ?: timer?.validate() ?: timer?.validate()
    }
}

data class ImageSliderTheme(
    val marker: ViewStyleProps? = null,
    val track: ViewStyleProps? = null
) : WidgetBaseThemeComponent() {
    override fun validate(): String? {
        return body?.validate() ?: dismiss?.validate() ?: footer?.validate()
        ?: header?.validate() ?: marker?.validate() ?: timer?.validate() ?: timer?.validate()
        ?: track?.validate()
    }
}

data class CheerMeterTheme(
    val sideABar: ViewStyleProps? = null,
    val sideAButton: ViewStyleProps? = null,
    val sideBBar: ViewStyleProps? = null,
    val sideBButton: ViewStyleProps? = null,
    val versus: ViewStyleProps? = null
) : WidgetBaseThemeComponent() {
    override fun validate(): String? {
        return body?.validate() ?: dismiss?.validate() ?: footer?.validate()
        ?: header?.validate()
        ?: sideABar?.validate() ?: sideAButton?.validate() ?: sideBBar?.validate()
        ?: sideAButton?.validate() ?: timer?.validate() ?: title?.validate()
        ?: versus?.validate()
    }
}

data class WidgetsTheme(
    val alert: WidgetBaseThemeComponent? = null,
    val cheerMeter: CheerMeterTheme? = null,
    val imageSlider: ImageSliderTheme? = null,
    val poll: OptionsWidgetThemeComponent? = null,
    val prediction: OptionsWidgetThemeComponent? = null,
    val predictionFollowUp: OptionsWidgetThemeComponent? = null,
    val quiz: OptionsWidgetThemeComponent? = null
) : BaseTheme() {
    override fun validate(): String? {
        return alert?.validate() ?: cheerMeter?.validate() ?: imageSlider?.validate() ?: poll?.validate()
        ?: prediction?.validate() ?: predictionFollowUp?.validate()
        ?: quiz?.validate()
    }

    fun getThemeLayoutComponent(widgetType: WidgetType): BaseTheme? {
        return when (widgetType) {
            WidgetType.ALERT -> alert
            WidgetType.TEXT_POLL, WidgetType.IMAGE_POLL -> poll
            WidgetType.TEXT_QUIZ, WidgetType.IMAGE_QUIZ -> quiz
            WidgetType.TEXT_PREDICTION, WidgetType.IMAGE_PREDICTION -> prediction
            WidgetType.TEXT_PREDICTION_FOLLOW_UP, WidgetType.IMAGE_PREDICTION_FOLLOW_UP -> predictionFollowUp
            WidgetType.IMAGE_SLIDER -> imageSlider
            WidgetType.CHEER_METER -> cheerMeter
            else -> null
        }
    }
}

data class ViewStyleProps(
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
        if (AndroidResource.getColorFromString(borderColor) == null)
            return "Unable to parse Border Color"
        if (AndroidResource.getColorFromString(fontColor) == null)
            return "Unable to parse Font Color"
        return background?.validate()
    }
}

data class OptionsWidgetThemeComponent(
    val correctOption: ViewStyleProps? = null,
    val correctOptionBar: ViewStyleProps? = null,
    val correctOptionDescription: ViewStyleProps? = null,
    val correctOptionImage: ViewStyleProps? = null,
    val correctOptionPercentage: ViewStyleProps? = null,
    val incorrectOption: ViewStyleProps? = null,
    val incorrectOptionBar: ViewStyleProps? = null,
    val incorrectOptionDescription: ViewStyleProps? = null,
    val incorrectOptionImage: ViewStyleProps? = null,
    val incorrectOptionPercentage: ViewStyleProps? = null,
    val selectedOption: ViewStyleProps? = null,
    val selectedOptionBar: ViewStyleProps? = null,
    val selectedOptionDescription: ViewStyleProps? = null,
    val selectedOptionImage: ViewStyleProps? = null,
    val selectedOptionPercentage: ViewStyleProps? = null,
    val unselectedOption: ViewStyleProps? = null,
    val unselectedOptionBar: ViewStyleProps? = null,
    val unselectedOptionDescription: ViewStyleProps? = null,
    val unselectedOptionImage: ViewStyleProps? = null,
    val unselectedOptionPercentage: ViewStyleProps? = null
) : WidgetBaseThemeComponent() {
    override fun validate(): String? {
        return super.validate() ?: correctOption?.validate() ?: correctOptionBar?.validate()
        ?: correctOptionDescription?.validate() ?: correctOptionImage?.validate()
        ?: correctOptionPercentage?.validate()
        ?: incorrectOption?.validate() ?: incorrectOptionBar?.validate()
        ?: incorrectOptionDescription?.validate() ?: incorrectOptionImage?.validate()
        ?: incorrectOptionPercentage?.validate() ?: selectedOption?.validate()
        ?: selectedOptionBar?.validate() ?: selectedOptionDescription?.validate()
        ?: selectedOptionImage?.validate() ?: selectedOptionPercentage?.validate()
        ?: unselectedOption?.validate()
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
        val colorCheck = AndroidResource.getColorFromString(color)
        var colorsCheck: String? = null
        colors?.let {
            for (color in it) {
                if (AndroidResource.getColorFromString(color) == null) {
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
