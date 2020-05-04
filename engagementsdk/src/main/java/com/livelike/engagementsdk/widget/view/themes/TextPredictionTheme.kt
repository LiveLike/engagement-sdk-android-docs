package com.livelike.engagementsdk.widget.view.themes

import com.livelike.engagementsdk.widget.BaseTheme
import com.livelike.engagementsdk.widget.Component

data class TextPredictionTheme(
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
):BaseTheme() {
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