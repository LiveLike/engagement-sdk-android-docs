package com.livelike.livelikedemo

import WidgetsTimeLineView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.core.services.messaging.proxies.LiveLikeWidgetEntity
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.widget.timeline.WidgetTimeLineViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import com.livelike.livelikedemo.customwidgets.timeline.TimeLineWidgetFactory
import kotlinx.android.synthetic.main.activity_live_blog.timeline_container
import kotlinx.android.synthetic.main.time_line_item.view.txt_index
import kotlinx.android.synthetic.main.time_line_item.view.txt_time
import kotlinx.android.synthetic.main.time_line_item.view.widget_view
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

class LiveBlogActivity : AppCompatActivity() {


    private lateinit var session: LiveLikeContentSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_blog)
        val channelManager = (application as LiveLikeApplication).channelManager
        val channel = channelManager.selectedChannel
        session = (application as LiveLikeApplication).createPublicSession(
            channel.llProgram.toString(),
            null
        )
        val timeLineViewModel = WidgetTimeLineViewModel(session)
        val timeLineView = WidgetsTimeLineView(
            this,
            timeLineViewModel,
            (application as LiveLikeApplication).sdk
        )
        if(LiveLikeApplication.showCustomWidgetsUI){
            timeLineView.widgetViewFactory = TimeLineWidgetFactory(this,timeLineViewModel.timeLineWidgets)
        }
        timeline_container.addView(timeLineView)
    }



    class TimeLineAdapter(private val application: LiveLikeApplication) :
        RecyclerView.Adapter<TimeLineViewHolder>() {

        init {
            setHasStableIds(true)
        }

        val list: ArrayList<LiveLikeWidget> = arrayListOf()
        var widgetStates: WidgetStates = WidgetStates.READY
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): TimeLineViewHolder {
            return TimeLineViewHolder(
                LayoutInflater.from(p0.context).inflate(R.layout.time_line_item, p0, false)
            )
        }

        override fun getItemViewType(position: Int): Int {
            return position
        }

        override fun onBindViewHolder(viewHolder: TimeLineViewHolder, p1: Int) {
            val liveLikeWidget = list[p1]
            viewHolder.itemView.widget_view.enableDefaultWidgetTransition = false
            viewHolder.itemView.widget_view.displayWidget(
                application.sdk,
                liveLikeWidget
            )
            viewHolder.itemView.txt_index.text = "$p1"
            viewHolder.itemView.widget_view.setState(widgetStates)
//            viewHolder.itemView.widget_view.moveToNextState()
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'")
            try {
                val date: Date = format.parse(liveLikeWidget.createdAt)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd")
                val dateTime = dateFormat.format(date)
                viewHolder.itemView.txt_time.text = dateTime
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            viewHolder.itemView.widget_view.widgetLifeCycleEventsListener =
                object : WidgetLifeCycleEventsListener() {
                    override fun onWidgetPresented(widgetData: LiveLikeWidgetEntity) {

                    }

                    override fun onWidgetInteractionCompleted(widgetData: LiveLikeWidgetEntity) {
                    }

                    override fun onWidgetDismissed(widgetData: LiveLikeWidgetEntity) {
                    }

                    override fun onWidgetStateChange(
                        state: WidgetStates,
                        widgetData: LiveLikeWidgetEntity
                    ) {
                        println("state = [${state}], widgetData = [${widgetData}]")
                    }

                    override fun onUserInteract(widgetData: LiveLikeWidgetEntity) {

                    }
                }

            println("TimeLineAdapter.onBindViewHolder->$p1 ->${liveLikeWidget.kind} ->$widgetStates ->${viewHolder.itemView.widget_view.getCurrentState()}")
        }

        override fun getItemCount(): Int = list.size

    }

    class TimeLineViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
