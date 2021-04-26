package com.livelike.livelikedemo

import com.livelike.engagementsdk.widget.timeline.WidgetsTimeLineView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.livelike.livelikedemo.customwidgets.timeline.TimeLineWidgetFactory
import com.livelike.livelikedemo.viewmodels.LiveBlogViewModel
import com.livelike.livelikedemo.viewmodels.LiveBlogModelFactory
import kotlinx.android.synthetic.main.activity_live_blog.timeline_container

class LiveBlogActivity : AppCompatActivity() {


    var liveBlogViewModel: LiveBlogViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_blog)

        /**
         * init view model, which will be responsible for managing WidgetTimelineViewmodel, so that its not created each time
         **/
        initViewModel()

        /**
         * create timeline view
         **/
        val timeLineView = WidgetsTimeLineView(
            this,
            liveBlogViewModel?.timeLineViewModel!!,
            liveBlogViewModel?.getEngagementSDK()!!
        )
        if(LiveLikeApplication.showCustomWidgetsUI){
            timeLineView.widgetViewFactory = TimeLineWidgetFactory(this,liveBlogViewModel?.timeLineViewModel!!.timeLineWidgets)
        }

        timeline_container.addView(timeLineView)
    }


    private fun initViewModel(){
        liveBlogViewModel = ViewModelProvider(
            this,
            LiveBlogModelFactory(this.application)
        ).get(LiveBlogViewModel::class.java)

    }


}
