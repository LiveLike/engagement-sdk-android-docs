package com.mml.mmlengagementsdk.widgets.timeline

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.OptionsItem
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import kotlinx.android.synthetic.main.mml_timeline_item.view.widget_view

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
        val timelineWidgetResource = list[p1]
        val liveLikeWidget = timelineWidgetResource.liveLikeWidget
        itemViewHolder.itemView.widget_view.widgetViewFactory =
            TimeLineWidgetFactory(
                context = context,
                widgetList = list
            )
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

data class TimelineWidgetResource(
    var isActive: Boolean = false,
    val liveLikeWidget: LiveLikeWidget,
    var selectedOptionitem: OptionsItem? = null,
    var liveLikeWidgetResult: LiveLikeWidgetResult? = null,
    var startTime : Long? = null
)