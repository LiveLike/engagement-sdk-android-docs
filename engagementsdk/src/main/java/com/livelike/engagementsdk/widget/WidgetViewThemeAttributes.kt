package com.livelike.engagementsdk.widget

import android.content.res.TypedArray
import com.livelike.engagementsdk.R

class WidgetViewThemeAttributes {

    var widgetWinAnimation: String = "winAnimation"
    var widgetLoseAnimation: String = "loseAnimation"
    var widgetDrawAnimation: String = "drawAnimation"

    fun init(typedArray: TypedArray?) {
        typedArray?.apply {
            widgetWinAnimation =
                getString(R.styleable.LiveLike_WidgetView_winAnimation) ?: "winAnimation"
            widgetLoseAnimation =
                getString(R.styleable.LiveLike_WidgetView_loseAnimation) ?: "loseAnimation"
            widgetDrawAnimation =
                getString(R.styleable.LiveLike_WidgetView_drawAnimation) ?: "drawAnimation"
        }
    }
}
