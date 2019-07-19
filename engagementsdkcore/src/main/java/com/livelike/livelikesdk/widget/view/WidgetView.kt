package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.LiveLikeContentSession

class WidgetView(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {
    fun setSession(session: LiveLikeContentSession) {
        session.setWidgetContainer(this)
    }
    fun pause(){

    }
    fun resume(){

    }
}