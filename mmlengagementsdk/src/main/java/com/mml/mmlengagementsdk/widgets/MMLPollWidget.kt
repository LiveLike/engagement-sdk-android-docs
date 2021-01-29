package com.mml.mmlengagementsdk.widgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.mml.mmlengagementsdk.widgets.adapter.PollListAdapter
import com.mml.mmlengagementsdk.widgets.timeline.TimelineWidgetResource
import com.mml.mmlengagementsdk.widgets.utils.getFormattedTime
import com.mml.mmlengagementsdk.widgets.utils.parseDuration
import com.mml.mmlengagementsdk.widgets.utils.setCustomFontWithTextStyle
import kotlinx.android.synthetic.main.mml_poll_widget.view.rcyl_poll_list
import kotlinx.android.synthetic.main.mml_poll_widget.view.time_bar
import kotlinx.android.synthetic.main.mml_poll_widget.view.txt_time
import kotlinx.android.synthetic.main.mml_poll_widget.view.txt_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.max

class MMLPollWidget(context: Context) : ConstraintLayout(context) {
    var pollWidgetModel: PollWidgetModel? = null
    var isImage = false
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    var timelineWidgetResource: TimelineWidgetResource? = null

    init {
        inflate(context, R.layout.mml_poll_widget, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pollWidgetModel?.widgetData?.let { liveLikeWidget ->
            txt_title.text = liveLikeWidget.question
            setCustomFontWithTextStyle(txt_title, "fonts/RingsideExtraWide-Black.otf")
            liveLikeWidget.createdAt?.let {
                setCustomFontWithTextStyle(txt_time, "fonts/RingsideRegular-Book.otf")
                txt_time.text = getFormattedTime(it)
            }
            liveLikeWidget.options?.let { list ->
                if (isImage) {
                    rcyl_poll_list.layoutManager = GridLayoutManager(context, 2)
                } else {
                    rcyl_poll_list.layoutManager =
                        LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                }
                val adapter =
                    PollListAdapter(
                        context,
                        isImage,
                        ArrayList(list.map { item -> item!! })
                    )
                rcyl_poll_list.adapter = adapter

                if (timelineWidgetResource?.isActive == false) {
                    adapter.isTimeLine = true
                    list.forEach { op ->
                        op?.let {
                            adapter.optionIdCount[op.id!!] = op.voteCount ?: 0
                        }
                    }
                    timelineWidgetResource?.liveLikeWidgetResult?.choices?.let { options ->
                        options.forEach { op ->
                            adapter.optionIdCount[op.id] = op.vote_count ?: 0
                        }
                    }
                    adapter.notifyDataSetChanged()
                    time_bar.visibility = View.INVISIBLE
                } else {
                    adapter.pollListener = object : PollListAdapter.PollListener {
                        override fun onSelectOption(id: String) {
                            pollWidgetModel?.submitVote(id)
                        }
                    }
                    pollWidgetModel?.voteResults?.subscribe(this@MMLPollWidget) { result ->
                        result?.choices?.let { options ->
                            var change = false
                            options.forEach { op ->
                                if (!adapter.optionIdCount.containsKey(op.id) || adapter.optionIdCount[op.id] != op.vote_count) {
                                    change = true
                                }
                                adapter.optionIdCount[op.id] = op.vote_count ?: 0
                            }
                            if (change)
                                adapter.notifyDataSetChanged()
                        }
                        timelineWidgetResource?.liveLikeWidgetResult = result
                    }

                    if (timelineWidgetResource?.startTime == null) {
                        timelineWidgetResource?.startTime = Calendar.getInstance().timeInMillis
                    }
                    val timeMillis = liveLikeWidget.timeout?.parseDuration() ?: 5000
                    val timeDiff =
                        Calendar.getInstance().timeInMillis - (timelineWidgetResource?.startTime
                            ?: 0L)
                    val remainingTimeMillis = max(0, timeMillis - timeDiff)
                    time_bar.visibility = View.VISIBLE
                    time_bar.startTimer(timeMillis, remainingTimeMillis)

                    uiScope.async {
                        delay(remainingTimeMillis)
                        timelineWidgetResource?.isActive = false
                        adapter.isTimeLine = true
                        adapter.notifyDataSetChanged()
                        pollWidgetModel?.voteResults?.unsubscribe(this@MMLPollWidget)
                    }
                }
            }
        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (timelineWidgetResource?.isActive == true) {
            job.cancel()
            uiScope.cancel()
            pollWidgetModel?.voteResults?.unsubscribe(this)
            pollWidgetModel?.finish()
        }
    }
}