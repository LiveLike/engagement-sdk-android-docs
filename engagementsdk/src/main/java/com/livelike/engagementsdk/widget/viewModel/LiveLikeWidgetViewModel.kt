package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.LiveLikeWidget

interface LiveLikeWidgetMediator {

    val widgetData : LiveLikeWidget

    fun dismissWidget(action: DismissAction)

}