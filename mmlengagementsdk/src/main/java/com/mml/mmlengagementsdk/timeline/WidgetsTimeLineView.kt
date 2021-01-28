package com.mml.mmlengagementsdk.timeline

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.OptionsItem
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.android.synthetic.main.mml_timeline_item.view.widget_view
import kotlinx.android.synthetic.main.mml_timeline_view.view.timeline_rv

class WidgetsTimeLineView(context: Context, val session : LiveLikeContentSession, val sdk : EngagementSDK) : FrameLayout(context) {

    private var adapter: TimeLineViewAdapter

    init {
        inflate(context, R.layout.mml_timeline_view, this)
        adapter =
            TimeLineViewAdapter(
                context,
                sdk
            )
        timeline_rv.adapter = adapter
        timeline_rv.layoutManager = LinearLayoutManager(context)
        initializePastPublishedWidgets()
    }


    private fun initializePastPublishedWidgets() {
        session.getPublishedWidgets(
            LiveLikePagination.FIRST,
            object : LiveLikeCallback<List<LiveLikeWidget>>() {
                override fun onResponse(result: List<LiveLikeWidget>?, error: String?) {
                    result?.let { list ->
                        adapter.list.addAll(list.map {  TimelineWidgetResource(false, it) })
                        adapter.notifyDataSetChanged()
                    }
                }
            })
    }

    private fun observeForLiveWidgets() {
        session.widgetStream.subscribe(this) {
            it?.let {
                adapter.list.add(0, TimelineWidgetResource(true, it))
                adapter.notifyDataSetChanged()
            }
        }

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        session.resume()
        observeForLiveWidgets()
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        session.pause()
        session.widgetStream.unsubscribe(this)
    }

    class TimeLineViewAdapter(private val context: Context, private val sdk: EngagementSDK) :
        RecyclerView.Adapter<TimeLineItemViewHolder>() {

        init {
            setHasStableIds(true)
        }

        val list: ArrayList<TimelineWidgetResource> = arrayListOf()

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): TimeLineItemViewHolder {
            return TimeLineItemViewHolder(
                LayoutInflater.from(p0.context).inflate(R.layout.mml_timeline_item, p0, false)
            )
        }

        override fun getItemViewType(position: Int): Int {
            return position
        }

        override fun onBindViewHolder(itemViewHolder: TimeLineItemViewHolder, p1: Int) {
            val liveLikeWidget = list[p1].liveLikeWidget
            itemViewHolder.itemView.widget_view.widgetViewFactory = TimeLineWidgetFactory(context = context ,widgetList = list)
            itemViewHolder.itemView.widget_view.enableDefaultWidgetTransition = false
            itemViewHolder.itemView.widget_view.displayWidget(
                sdk,
                liveLikeWidget
            )
        }

        override fun getItemCount(): Int = list.size

        override fun getItemId(position: Int): Long {
            return list[position].liveLikeWidget.id.hashCode().toLong()
        }
    }

    class TimeLineItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    data class TimelineWidgetResource (var isActive:Boolean = false, val liveLikeWidget: LiveLikeWidget, var selectedOptionitem : OptionsItem?=null)

}