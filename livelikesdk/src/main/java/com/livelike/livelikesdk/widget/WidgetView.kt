package com.livelike.livelikesdk.widget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.R

class WidgetView(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs), WidgetRenderer {
    override var widgetListener : WidgetEventListener? = null
    private var container : FrameLayout

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_view, this, true)
        container = findViewById(R.id.containerView)
    }

    fun setSession(liveLikeContentSession: LiveLikeContentSession) {
        liveLikeContentSession.renderer = this
    }

    // TODO: Once we start receiving the events from pubnub, below code will move to displayWidget()
    @SuppressLint("ClickableViewAccessibility")
    override fun onFinishInflate() {
        super.onFinishInflate()
        Handler().postDelayed({
            val layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            layoutParams.topMargin = 0
            val predictionWidget = PredictionTextWidgetView(context, null, 0)
            predictionWidget.layoutParams = layoutParams
            container.addView(predictionWidget)
        }, resources.getInteger(R.integer.prediction_widget_question_trigger_time_in_milliseconds).toLong())
    }

    override fun displayWidget(widgetData: Any) {

    }
}

