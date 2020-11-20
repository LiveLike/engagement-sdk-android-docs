package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.LiveLikeWidget

interface LiveLikeWidgetMediator {

    /**
     * widget data holder
     */
    val widgetData : LiveLikeWidget

    /**
     * call this to cleanup the viewModel and its association
     */
    fun finish()

}