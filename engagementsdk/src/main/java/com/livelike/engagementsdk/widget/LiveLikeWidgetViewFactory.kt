package com.livelike.engagementsdk.widget

import android.view.View
import com.livelike.engagementsdk.widget.viewModel.AlertWidgetModel
import com.livelike.engagementsdk.widget.viewModel.CheerMeterWidgetmodel

interface LiveLikeWidgetViewFactory {


    fun createCheerMeterView(cheerMeterWidgetModel: CheerMeterWidgetmodel
    ) : View?

    fun createAlertWidgetView(alertWidgetModel: AlertWidgetModel
    ) : View?



}