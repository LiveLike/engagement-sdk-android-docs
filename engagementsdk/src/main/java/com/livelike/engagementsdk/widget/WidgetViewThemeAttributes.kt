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
                getString(R.styleable.WidgetView_quizCorrect) ?: "correctAnswer"
            widgetQuizInCorrectAnimation =
                getString(R.styleable.WidgetView_quizInCorrect) ?: "wrongAnswer"
            widgetCheerMeterDrawAnimation =
                getString(R.styleable.WidgetView_cheerMeterDrawAnimation) ?: "drawAnimation"
            widgetCheerMeterLoserAnimation =
                getString(R.styleable.WidgetView_cheerMeterLoserAnimation) ?: "loserAnimation"
            widgetCheerMeterWinnerAnimation =
                getString(R.styleable.WidgetView_cheerMeterWinnerAnimation) ?: "winnerAnimation"
            println("SD--> $widgetCheerMeterWinnerAnimation -- $widgetCheerMeterLoserAnimation -- $widgetCheerMeterDrawAnimation -- $widgetQuizCorrectAnimation -- $widgetQuizInCorrectAnimation")
        }
    }

}