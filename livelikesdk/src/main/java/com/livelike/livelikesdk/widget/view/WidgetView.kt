package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.widget.WidgetEvent


open class WidgetView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs){
    private var container : FrameLayout

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_view, this, true)
        container = findViewById(R.id.containerView)
    }
}

internal interface WidgetEventListener {
    fun onAnalyticsEvent(data: Any)
    fun onWidgetEvent(event: WidgetEvent)
    fun onOptionVote(voteUrl: String)
}