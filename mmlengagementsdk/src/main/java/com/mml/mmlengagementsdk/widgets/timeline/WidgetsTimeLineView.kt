package com.mml.mmlengagementsdk.widgets.timeline

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.FrameLayout
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.android.synthetic.main.mml_timeline_view.view.timeline_rv

class WidgetsTimeLineView(
    context: Context,
    val session: LiveLikeContentSession,
    val sdk: EngagementSDK
) : FrameLayout(context) {

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
        timeline_rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var loading: Boolean = true
            var pastVisiblesItems = 0
            var visibleItemCount: Int = 0
            var totalItemCount: Int = 0
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) { //check for scroll down
                    visibleItemCount = timeline_rv.layoutManager!!.childCount
                    totalItemCount = timeline_rv.layoutManager!!.itemCount
                    pastVisiblesItems =
                        (timeline_rv.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

                    if (loading) {
                        if ((visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                            loading = false
                            loadPastPublishedWidgets(LiveLikePagination.NEXT)
                            loading = true
                        }
                    }
                }
            }
        })

        loadPastPublishedWidgets(LiveLikePagination.FIRST)
    }

    private fun loadPastPublishedWidgets(liveLikePagination: LiveLikePagination) {
        session.getPublishedWidgets(
            liveLikePagination,
            object : LiveLikeCallback<List<LiveLikeWidget>>() {
                override fun onResponse(result: List<LiveLikeWidget>?, error: String?) {
                    result?.let { list ->
                        adapter.list.addAll(list.map { TimelineWidgetResource(false, it) })
                        adapter.notifyDataSetChanged()
                    }
                }
            })
    }

    private fun observeForLiveWidgets() {
        session.widgetStream.subscribe(this) {
            it?.let {
                adapter.list.add(0, TimelineWidgetResource(true, it))
                Handler(Looper.getMainLooper()).post {
                    adapter.notifyDataSetChanged()
                }
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
        println("WidgetsTimeLineView.onDetachedFromWindow")
        session.pause()
        session.widgetStream.unsubscribe(this)
    }


}