package com.mml.mmlengagementsdk.widgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.mml.mmlengagementsdk.widgets.adapter.PollListAdapter
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
import kotlinx.coroutines.delay

class MMLPollWidget : ConstraintLayout {
    var pollWidgetModel: PollWidgetModel? = null
    var isImage = false
    var isTimeLine = false
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
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
                        ArrayList(list.map { item -> item!! }),
                        isTimeLine
                    )
                rcyl_poll_list.adapter = adapter

                if (isTimeLine) {
                    list.forEach { op ->
                        op?.let {
                            adapter.optionIdCount[op.id!!] = op.voteCount ?: 0
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
                    pollWidgetModel?.voteResults?.subscribe(this) { result ->
                        result?.choices?.let { options ->
                            options.forEach { op ->
                                adapter.optionIdCount[op.id] = op.vote_count ?: 0
                            }
                            adapter.notifyDataSetChanged()
                        }
                    }
                    val timeMillis = liveLikeWidget.timeout?.parseDuration() ?: 5000
                    time_bar.startTimer(timeMillis)

                    uiScope.async {
                        delay(timeMillis)
                        pollWidgetModel?.finish()
                    }
                }
            }
        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pollWidgetModel?.voteResults?.unsubscribe(this)
        pollWidgetModel?.finish()
    }
}