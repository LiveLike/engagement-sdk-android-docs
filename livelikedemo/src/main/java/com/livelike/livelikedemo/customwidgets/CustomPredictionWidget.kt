package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.PredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.widgetModel.FollowUpWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.PredictionWidgetViewModel
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_poll_widget.view.button2
import kotlinx.android.synthetic.main.custom_poll_widget.view.imageView2
import kotlinx.android.synthetic.main.custom_poll_widget.view.rcyl_poll_list

class CustomPredictionWidget :
    ConstraintLayout {
    var predictionWidgetViewModel: PredictionWidgetViewModel? = null
    var followUpWidgetViewModel: FollowUpWidgetViewModel? = null
    var isImage = false
    var isFollowUp = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        inflate(context, R.layout.custom_prediction_widget, this@CustomPredictionWidget)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        var widgetData = predictionWidgetViewModel?.widgetData
        var voteResults = predictionWidgetViewModel?.voteResults
        if (isFollowUp) {
            widgetData = followUpWidgetViewModel?.widgetData
            voteResults = followUpWidgetViewModel?.voteResults
        }
        predictionWidgetViewModel?.loadInteractionHistory(object :
            LiveLikeCallback<List<PredictionWidgetUserInteraction>>() {
            override fun onResponse(
                result: List<PredictionWidgetUserInteraction>?,
                error: String?
            ) {
                result?.forEach {
                    println("CustomPredictionWidget.onResponse>>${it.optionId} =>${it.isCorrect}")
                }
            }

        })

        widgetData?.let { liveLikeWidget ->
            liveLikeWidget.options?.let {
                if (it.size > 2) {
                    rcyl_poll_list.layoutManager =
                        GridLayoutManager(
                            context,
                            2
                        )
                }
                val adapter =
                    PollListAdapter(context, isImage, ArrayList(it.map { item -> item!! }))
                rcyl_poll_list.adapter = adapter
                adapter.pollListener = object : PollListAdapter.PollListener {
                    override fun onSelectOption(id: String) {
                        predictionWidgetViewModel?.lockInVote(id)
                    }
                }
                button2.visibility = View.GONE
                voteResults?.subscribe(this) { result ->
                    result?.choices?.let { options ->
                        options.forEach { op ->
                            adapter.optionIdCount[op.id] = op.vote_count ?: 0
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
                if (isFollowUp) {
                    it.forEach { op ->
                        adapter.optionIdCount[op?.id!!] = op.voteCount ?: 0
                    }
                    adapter.isFollowUp = true
                    adapter.selectedIndex = it.indexOfFirst { option -> option?.id == followUpWidgetViewModel?.getPredictionVoteId() }
                    adapter.notifyDataSetChanged()
                    followUpWidgetViewModel?.claimRewards()
                }
            }
            imageView2.setOnClickListener {
                finish()
            }

//            val handler = Handler()
//            handler.postDelayed(
//                {
//                    finish()
//                },
//                (liveLikeWidget.timeout ?: "").parseDuration()
//            )
        }
    }

    private fun finish() {
        predictionWidgetViewModel?.finish()
        followUpWidgetViewModel?.finish()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        predictionWidgetViewModel?.voteResults?.unsubscribe(this)
    }
}
