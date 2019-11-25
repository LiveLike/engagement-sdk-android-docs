package com.livelike.engagementsdk.widget

import android.content.Context
import android.content.res.TypedArray
import com.livelike.engagementsdk.R

class WidgetViewThemeAttributes {

    var widgetQuizCorrectAnimation: String = "correctAnswer"
    var widgetQuizInCorrectAnimation: String = "wrongAnswer"

    fun init(context: Context, typedArray: TypedArray?) {
        typedArray?.apply {
            widgetQuizCorrectAnimation =
                getString(R.styleable.WidgetView_quizCorrect) ?: "correctAnswer"
            widgetQuizInCorrectAnimation =
                getString(R.styleable.WidgetView_quizInCorrect) ?: "wrongAnswer"
        }
    }

}