package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.NumberPredictionViewModel

class NumberPredictionView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr)  {
    private var viewModel: NumberPredictionViewModel? = null
    private var isFirstInteraction = false

    override var widgetViewModel: BaseViewModel? = null
        get() = viewModel
        set(value) {
            field = value
            viewModel = value as NumberPredictionViewModel
        }

    init {
        isFirstInteraction = viewModel?.getUserInteraction() != null
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}