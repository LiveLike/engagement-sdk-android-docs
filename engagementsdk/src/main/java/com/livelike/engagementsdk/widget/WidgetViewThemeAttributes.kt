package com.livelike.engagementsdk.widget

import android.content.res.TypedArray
import com.livelike.engagementsdk.R

class WidgetViewThemeAttributes {

    var widgetQuizCorrectAnimation: String = "correctAnswer"
    var widgetQuizInCorrectAnimation: String = "wrongAnswer"
    var widgetCheerMeterWinnerAnimation: String = "winnerAnimation"
    var widgetCheerMeterLoserAnimation: String = "loserAnimation"
    var widgetCheerMeterDrawAnimation: String = "drawAnimation"

    fun init(typedArray: TypedArray?) {
        typedArray?.apply {
            widgetQuizCorrectAnimation =
                getString(R.styleable.WidgetView_winAnimation) ?: "correctAnswer"
            widgetQuizInCorrectAnimation =
                getString(R.styleable.WidgetView_loseAnimation) ?: "wrongAnswer"
            widgetCheerMeterDrawAnimation =
                getString(R.styleable.WidgetView_drawAnimation) ?: "drawAnimation"
            widgetCheerMeterLoserAnimation =
                getString(R.styleable.WidgetView_loseAnimation) ?: "loserAnimation"
            widgetCheerMeterWinnerAnimation =
                getString(R.styleable.WidgetView_winAnimation) ?: "winnerAnimation"
        }
    }

}