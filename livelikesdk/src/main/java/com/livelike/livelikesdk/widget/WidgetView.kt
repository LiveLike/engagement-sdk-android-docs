package com.livelike.livelikesdk.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.widget.Toast
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.R

import java.util.*

class WidgetView(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs), WidgetRenderer {
    override var widgetListener : WidgetEventListener? = null

    companion object {
        const val AUTO_DISMISS_DELAY = 5000L
    }


    fun setSession(liveLikeContentSession: LiveLikeContentSession) {
        liveLikeContentSession.renderer = this
    }

    override fun displayWidget(widgetData: Any) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, widgetData.toString(), Toast.LENGTH_SHORT).show()
            val timerTask = object : TimerTask() {
                override fun run() {
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                }
            }
            Timer().schedule(timerTask, AUTO_DISMISS_DELAY)
        }
    }
}

