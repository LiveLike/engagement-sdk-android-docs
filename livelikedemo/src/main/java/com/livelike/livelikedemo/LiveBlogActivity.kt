package com.livelike.livelikedemo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.widget.timeline.CMSSpecifiedDurationTimer
import com.livelike.engagementsdk.widget.timeline.WidgetTimerController
import com.livelike.engagementsdk.widget.timeline.WidgetsTimeLineView
import com.livelike.livelikedemo.customwidgets.timeline.TimeLineWidgetFactory
import com.livelike.livelikedemo.utils.ThemeRandomizer
import com.livelike.livelikedemo.viewmodels.LiveBlogModelFactory
import com.livelike.livelikedemo.viewmodels.LiveBlogViewModel
import kotlinx.android.synthetic.main.activity_live_blog.radio_group
import kotlinx.android.synthetic.main.activity_live_blog.timeline_container
import kotlinx.android.synthetic.main.activity_live_blog.timeout_set
import kotlinx.android.synthetic.main.activity_live_blog.widget_timeout

open class LiveBlogActivity : AppCompatActivity() {

    open var liveBlogViewModel: LiveBlogViewModel? = null
    var timeout: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_blog)

        /**
         * init view model, which will be responsible for managing WidgetTimelineViewmodel, so that its not created each time
         **/
        initViewModel()

        radio_group.setOnCheckedChangeListener { _, checkedId ->
            liveBlogViewModel?.showAlertOnly = (checkedId == R.id.radio2)
            createTimeLineView()
        }
        createTimeLineView()
        timeout_set.setOnClickListener {
            Log.d("timeline", "timeout set")
            setWidgetTimeout()
        }
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

        if (!this.timeout.isNullOrEmpty()) {
            // integrator timeout configured
            timeLineView.widgetTimerController = object : WidgetTimerController() {
                override fun timeValue(widget: LiveLikeWidget): String {
                    return "P0DT00H00M${this@LiveBlogActivity.timeout}S"
                }
            }
        } else {
            // CMS timeout configured
            timeLineView.widgetTimerController = CMSSpecifiedDurationTimer()
        }

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

    private fun setWidgetTimeout() {

        if (!widget_timeout.text.toString().isNullOrEmpty()) {
            this.timeout = widget_timeout.text.toString()
            createTimeLineView()
        }
    }

    private fun initViewModel() {
        liveBlogViewModel = ViewModelProvider(
            this,
            LiveBlogModelFactory(this.application)
        ).get(LiveBlogViewModel::class.java)
    }
}
