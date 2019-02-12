package com.livelike.livelikesdk.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.messaging.*

class WidgetView(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs), MessagingEventListener {
    override fun onClientMessageError(client: MessagingClient, error: Error) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var container : FrameLayout
    init {
        LayoutInflater.from(context).inflate(R.layout.widget_view, this, true)
        container = findViewById(R.id.containerView)
    }

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
}

