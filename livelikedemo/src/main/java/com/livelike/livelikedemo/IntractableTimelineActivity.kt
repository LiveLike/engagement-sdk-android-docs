package com.livelike.livelikedemo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.gson.JsonParser
import com.livelike.engagementsdk.LiveLikeEngagementTheme
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.widget.timeline.WidgetsTimeLineView
import com.livelike.livelikedemo.customwidgets.timeline.TimeLineWidgetFactory
import com.livelike.livelikedemo.utils.ThemeRandomizer
import com.livelike.livelikedemo.viewmodels.IntractableTimelineViewModelFactory
import com.livelike.livelikedemo.viewmodels.NewIntractableTimelineViewModel
import kotlinx.android.synthetic.main.activity_live_blog.radio1
import kotlinx.android.synthetic.main.activity_live_blog.radio2
import kotlinx.android.synthetic.main.activity_live_blog.timeline_container
import kotlinx.android.synthetic.main.activity_live_blog.timeout_layout

class IntractableTimelineActivity : AppCompatActivity() {

    var newTimelineViewModel: NewIntractableTimelineViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_blog)

        /**
         * Intractable view model initialized
         **/
        timeout_layout.visibility = View.GONE
        newTimelineViewModel = ViewModelProvider(
            this,
            IntractableTimelineViewModelFactory(this.application)
        ).get(NewIntractableTimelineViewModel::class.java)

        radio1.visibility = View.GONE
        radio2.visibility = View.GONE

      /*  radio_group.setOnCheckedChangeListener { group, checkedId ->
            newTimelineViewModel?.showAlertOnly = (checkedId == R.id.radio2)
            createTimeLineView()
        }*/
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
            newTimelineViewModel?.timeLineViewModel!!,
            newTimelineViewModel?.getEngagementSDK()!!
        )

        // added test theme
      /*  val themeFileName = "themes/test.json"
        val bufferReader = application.assets.open(themeFileName).bufferedReader()
        val data = bufferReader.use {
            it.readText()
        }
        val element =
            LiveLikeEngagementTheme.instanceFrom(JsonParser.parseString(data).asJsonObject)
        if (element is Result.Success)
            timeLineView.applyTheme(element.data)*/

        // adding custom separator between widgets in timeline
        timeLineView.setSeparator(ContextCompat.getDrawable(this, R.drawable.white_separator))

        if (LiveLikeApplication.showCustomWidgetsUI) {
            timeLineView.widgetViewFactory =
                TimeLineWidgetFactory(
                    this,
                    newTimelineViewModel?.timeLineViewModel!!.timeLineWidgets
                )
        } else {
            if (ThemeRandomizer.themesList.size > 0) {
                timeLineView.applyTheme(ThemeRandomizer.themesList.last())
            }
        }
        timeline_container.addView(timeLineView)
    }
}
