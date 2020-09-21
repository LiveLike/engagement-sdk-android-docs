package com.livelike.livelikedemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.services.messaging.proxies.LiveLikeWidgetEntity
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.activity_live_blog.btn_load_more
import kotlinx.android.synthetic.main.activity_live_blog.progress_bar
import kotlinx.android.synthetic.main.activity_live_blog.radio_finished
import kotlinx.android.synthetic.main.activity_live_blog.radio_interaction
import kotlinx.android.synthetic.main.activity_live_blog.radio_ready
import kotlinx.android.synthetic.main.activity_live_blog.radio_result
import kotlinx.android.synthetic.main.activity_live_blog.rcyl_timeline
import kotlinx.android.synthetic.main.time_line_item.view.txt_index
import kotlinx.android.synthetic.main.time_line_item.view.txt_time
import kotlinx.android.synthetic.main.time_line_item.view.widget_view
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

class LiveBlogActivity : AppCompatActivity() {

    private lateinit var adapter: TimeLineAdapter
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
        adapter = TimeLineAdapter((application as LiveLikeApplication))
        rcyl_timeline.adapter = adapter
        btn_load_more.setOnClickListener {
            loadData(LiveLikePagination.NEXT)
        }
        radio_ready.isChecked = true
        radio_ready.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                adapter.widgetStates = WidgetStates.READY
                adapter.notifyDataSetChanged()
            }
        }
        radio_interaction.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                adapter.widgetStates = WidgetStates.INTERACTING
                adapter.notifyDataSetChanged()
            }
        }
        radio_result.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                adapter.widgetStates = WidgetStates.RESULTS
                adapter.notifyDataSetChanged()
            }
        }
        radio_finished.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                adapter.widgetStates = WidgetStates.FINISHED
                adapter.notifyDataSetChanged()
            }
        }
        loadData(LiveLikePagination.FIRST)
    }

    private fun loadData(liveLikePagination: LiveLikePagination) {
        progress_bar.visibility = View.VISIBLE
        session.getPublishedWidgets(
            liveLikePagination,
            object : LiveLikeCallback<List<LiveLikeWidget?>>() {
                override fun onResponse(result: List<LiveLikeWidget?>?, error: String?) {
                    result?.let { list ->
                        adapter.list.addAll(list.map { it!! })
                        adapter.notifyDataSetChanged()
                    }
                }
            })
        progress_bar.visibility = View.GONE
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
                }
            println("TimeLineAdapter.onBindViewHolder->$p1 ->${liveLikeWidget.kind} ->$widgetStates ->${viewHolder.itemView.widget_view.getCurrentState()}")
        }

        override fun getItemCount(): Int = list.size

    }

    class TimeLineViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
