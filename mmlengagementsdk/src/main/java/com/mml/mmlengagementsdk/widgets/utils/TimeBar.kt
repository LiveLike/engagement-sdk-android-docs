package com.mml.mmlengagementsdk.widgets.utils

import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import kotlinx.android.synthetic.main.mml_alert_widget.view.time_bar

class TimeBar(context: Context) : View(context) {

    fun startTimer(totalTime: Long, remainingTime: Long) {
        time_bar.pivotX = 0f
        val scaleX = (totalTime - remainingTime) / totalTime.toFloat()
        ObjectAnimator.ofFloat(time_bar, "scaleX", scaleX, 1f).apply {
            this.duration = remainingTime
            start()
        }
    }


}