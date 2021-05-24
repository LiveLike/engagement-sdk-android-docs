package com.livelike.livelikedemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.livelike.engagementsdk.widget.timeline.CMSSpecifiedDurationTimer
import com.livelike.engagementsdk.widget.timeline.WidgetsTimeLineView
import com.livelike.livelikedemo.customwidgets.timeline.TimeLineWidgetFactory
import com.livelike.livelikedemo.utils.ThemeRandomizer
import com.livelike.livelikedemo.viewmodels.LiveBlogModelFactory
import com.livelike.livelikedemo.viewmodels.LiveBlogViewModel
import kotlinx.android.synthetic.main.activity_live_blog.radio_group
import kotlinx.android.synthetic.main.activity_live_blog.timeline_container

open class LiveBlogActivity : AppCompatActivity() {

    open var liveBlogViewModel: LiveBlogViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_blog)

        /**
         * init view model, which will be responsible for managing WidgetTimelineViewmodel, so that its not created each time
         **/
        initViewModel()


        radio_group.setOnCheckedChangeListener { group, checkedId ->
            liveBlogViewModel?.showAlertOnly = (checkedId == R.id.radio2)
            createTimeLineView()
        }
        createTimeLineView()
    }

    /**
     * create timeline view
     * for both the filter and non filtered widgets new instance of timeline and timelineviewmodel are created.
     **/
    private fun createTimeLineView() {
        timeline_container.removeAllViews()
        val timeLineView = WidgetsTimeLineView(
            this,
            liveBlogViewModel?.timeLineViewModel!!,
            liveBlogViewModel?.getEngagementSDK()!!
        )

        // adding custom separator between widgets in timeline
        timeLineView.setSeparator(ContextCompat.getDrawable(this, R.drawable.custom_separator_timeline))

        //CMS timer configured
        timeLineView.widgetTimerController = CMSSpecifiedDurationTimer()


        if (LiveLikeApplication.showCustomWidgetsUI) {
            timeLineView.widgetViewFactory =
                TimeLineWidgetFactory(
                    this,
                    liveBlogViewModel?.timeLineViewModel!!.timeLineWidgets
                )
        } else {
            if (ThemeRandomizer.themesList.size > 0) {
                timeLineView.applyTheme(ThemeRandomizer.themesList.last())
            }
        }
        timeline_container.addView(timeLineView)
    }



    private fun initViewModel() {
        liveBlogViewModel = ViewModelProvider(
            this,
            LiveBlogModelFactory(this.application)
        ).get(LiveBlogViewModel::class.java)
    }


}
