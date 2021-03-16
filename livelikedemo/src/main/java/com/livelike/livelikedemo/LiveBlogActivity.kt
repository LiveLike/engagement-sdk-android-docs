package com.livelike.livelikedemo

import WidgetsTimeLineView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.widget.timeline.WidgetTimeLineViewModel
import com.livelike.livelikedemo.customwidgets.timeline.TimeLineWidgetFactory
import kotlinx.android.synthetic.main.activity_live_blog.timeline_container

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


}
