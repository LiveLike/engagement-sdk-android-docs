package com.mml.mmlengagementsdk.widgets.utils

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.mml_alert_widget.view.time_bar

class TimeBar : View {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )


    fun startTimer(time: Long) {
//            time_bar.measure( View.MeasureSpec.EXACTLY, View.MeasureSpec.EXACTLY)
        time_bar.pivotX = 0f
        ObjectAnimator.ofFloat(time_bar, "scaleX", 0f, 1f).apply {
            this.duration = time
            start()
        }
    }

    fun startTimer(totalTime: Long, remainingTime: Long) {
        time_bar.pivotX = 0f
        val scaleX = (totalTime - remainingTime) / totalTime.toFloat()
        ObjectAnimator.ofFloat(time_bar, "scaleX", scaleX, 1f).apply {
            this.duration = remainingTime
            start()
        }
    }


}