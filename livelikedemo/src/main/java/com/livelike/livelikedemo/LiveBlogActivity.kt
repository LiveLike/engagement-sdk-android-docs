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
import com.livelike.livelikedemo.databinding.ActivityLiveBlogBinding
import com.livelike.livelikedemo.utils.ThemeRandomizer
import com.livelike.livelikedemo.viewmodels.LiveBlogModelFactory
import com.livelike.livelikedemo.viewmodels.LiveBlogViewModel
//import kotlinx.android.synthetic.main.activity_live_blog.*

open class LiveBlogActivity : AppCompatActivity() {

    open var liveBlogViewModel: LiveBlogViewModel? = null
    var timeout: String? = null
    private lateinit var binding: ActivityLiveBlogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveBlogBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        /**
         * init view model, which will be responsible for managing WidgetTimelineViewmodel, so that its not created each time
         **/
        initViewModel()

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            liveBlogViewModel?.showAlertOnly = (checkedId == R.id.radio2)
            createTimeLineView()
        }
        createTimeLineView()
        binding.timeoutSet.setOnClickListener {
            Log.d("timeline", "timeout set")
            setWidgetTimeout()
        }
    }

    /**
     * create timeline view
     * for both the filter and non filtered widgets new instance of timeline and timelineviewmodel are created.
     **/
    private fun createTimeLineView() {
        binding.timelineContainer.removeAllViews()

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
        binding.timelineContainer.addView(timeLineView)
    }

    private fun setWidgetTimeout() {

        if (!binding.widgetTimeout.text.toString().isNullOrEmpty()) {
            this.timeout = binding.widgetTimeout.text.toString()
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
