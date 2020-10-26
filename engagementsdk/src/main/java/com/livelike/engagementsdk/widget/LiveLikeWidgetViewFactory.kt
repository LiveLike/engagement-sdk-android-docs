package com.livelike.engagementsdk.widget

import android.view.View
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator

interface LiveLikeWidgetViewFactory {

    fun getWidgetView(widgetType : WidgetType, widgetData: LiveLikeWidget,
                      widgetMediator : LiveLikeWidgetMediator
    ) : View?

}