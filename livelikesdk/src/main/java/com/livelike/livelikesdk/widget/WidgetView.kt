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
import com.livelike.livelikesdk.widget.model.FollowupWidgetData
import com.livelike.livelikesdk.widget.model.PredictionWidgetData
import com.livelike.livelikesdk.widget.model.WidgetOptionsData
import com.livelike.livelikesdk.widget.model.WidgetData
import java.net.URI
import java.util.*

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

    var questionWidgetData = PredictionWidgetData()
    var questionWidgetDataList = mutableListOf<WidgetData>()
    val optionList = listOf(
            WidgetOptionsData(
                    UUID.fromString("0de19009-1a2b-4924-a0d2-7518fc44a6db"),
                    URI.create("https://livelike-blast.herokuapp.com/"),
                    "Sidney Crosby",
                    211
            ),
            WidgetOptionsData(
                    UUID.fromString("d49504eb-89f4-4752-9ad1-ec41dcc21c8a"),
                    URI.create("https://livelike-blast.herokuapp.com/"),
                    "Alexander Ovechkin",
                    611
            ),
            WidgetOptionsData(
                    UUID.fromString("e49504eb-89f4-4752-9ad1-ec41dcc21c8a"),
                    URI.create("https://livelike-blast.herokuapp.com/"),
                    "Parel Anthony",
                    411
            )
    )

    // TODO: Once we start receiving the events from pubnub, below code will move to displayWidget()
    @SuppressLint("ClickableViewAccessibility")
    override fun onFinishInflate() {
        super.onFinishInflate()
        val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.topMargin = 0


        Handler().postDelayed({
            val predictionWidget = PredictionTextQuestionWidgetView(context, null, 0)
            predictionWidget.layoutParams = layoutParams
            questionWidgetData.registerObserver(predictionWidget)
            questionWidgetData.question = "Who will be the winner of the match?"
            questionWidgetData.optionList = optionList
            questionWidgetDataList.add(questionWidgetData)
            container.addView(predictionWidget)
            // Unregister when view is removed.
        }, 2000)

        Handler().postDelayed({
            val predictionWidget = PredictionTextFollowUpWidgetView(context, null, 0)
            predictionWidget.layoutParams = layoutParams
            val followupWidgetData = FollowupWidgetData(questionWidgetDataList)
            followupWidgetData.registerObserver(predictionWidget)

            followupWidgetData.question = "Who will be the winner of the match?"
            followupWidgetData.correctOptionId = UUID.fromString("d49504eb-89f4-4752-9ad1-ec41dcc21c8a")
            followupWidgetData.optionList = optionList

            container.addView(predictionWidget)
        }, 13000)
    }

    override fun displayWidget(widgetData: WidgetData) {

    }

    override fun dismissCurrentWidget() {
        container.removeAllViews() // TODO: Use the dismiss method when MSDK-103 is implemented
    }
}

interface Observer {
    fun questionUpdated(questionText: String)
    fun optionListUpdated(optionList: Map<String, Long>,
                          optionSelectedCallback: (CharSequence?) -> Unit,
                          userSelection: Pair<String?, String?>)
    fun optionSelectedUpdated(selectedOption: WidgetOptionsData)
}