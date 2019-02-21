package com.livelike.livelikesdk.widget.view

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
import com.livelike.livelikesdk.widget.WidgetEventListener
import com.livelike.livelikesdk.widget.WidgetRenderer
import com.livelike.livelikesdk.util.WidgetTestData
import com.livelike.livelikesdk.widget.model.PredictionWidgetFollowUpData
import com.livelike.livelikesdk.widget.model.PredictionWidgetQuestionData
import com.livelike.livelikesdk.widget.model.WidgetData

class WidgetView(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs),
    WidgetRenderer {

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
        val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        layoutParams.topMargin = 0

        Handler().postDelayed({
            val predictionWidget = PredictionTextQuestionWidgetView(context, null, 0)
            predictionWidget.layoutParams = layoutParams
            val questionWidgetData = PredictionWidgetQuestionData()
            questionWidgetData.registerObserver(predictionWidget)
            WidgetTestData.fillDummyDataInTextPredictionQuestionData(questionWidgetData)
            container.addView(predictionWidget)
        }, resources.getInteger(R.integer.prediction_widget_question_trigger_time_in_milliseconds).toLong())

        Handler().postDelayed({
            val predictionWidget =
                PredictionTextFollowUpWidgetView(context, null, 0)
            predictionWidget.layoutParams = layoutParams
            val followupWidgetData = PredictionWidgetFollowUpData(WidgetTestData.questionWidgetDataList)
            followupWidgetData.registerObserver(predictionWidget)
            WidgetTestData.fillDummyDataInTextPredictionFollowUpData(followupWidgetData)
            container.addView(predictionWidget)
        }, resources.getInteger(R.integer.prediction_widget_followup_trigger_time_in_milliseconds).toLong())
    }

    override fun displayWidget(widgetData: WidgetData) {

    }

    override fun dismissCurrentWidget() {
        container.removeAllViews() // TODO: Use the dismiss method when MSDK-103 is implemented
    }
}