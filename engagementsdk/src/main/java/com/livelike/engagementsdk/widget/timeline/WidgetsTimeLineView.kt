
import android.content.Context
import android.widget.FrameLayout
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.widget.timeline.WidgetTimeLineViewModel
import kotlinx.android.synthetic.main.livelike_timeline_view.view.timeline_rv

class WidgetsTimeLineView(
    context: Context,
    private val timeLineViewModel: WidgetTimeLineViewModel,
    sdk: EngagementSDK
) : FrameLayout(context) {

    private var adapter: TimeLineViewAdapter


    init {
        inflate(context, R.layout.livelike_timeline_view, this)
        adapter =
            TimeLineViewAdapter(
                context,
                sdk
            )
        adapter.list.addAll(timeLineViewModel.timeLineWidgets)
        timeline_rv.adapter = adapter

    }

//    /**
//     * use this function to set timeline view model for the timeline view
//     * make sure to clear this timeline model when scope destroys
//     */
//    fun setTimeLineViewModel(timeLineViewModel: WidgetTimeLineViewModel) {
//        timeLineViewModel.clear()
//        this.timeLineViewModel = timeLineViewModel
//
//    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        subscribeForTimelineWidgets()
    }

    private fun subscribeForTimelineWidgets() {
        timeLineViewModel.timeLineWidgetsStream.subscribe(this) { widgets ->
            widgets?.let {
                adapter.list.addAll(widgets)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unsubscribeForTimelineWidgets()
    }

    private fun unsubscribeForTimelineWidgets() {
        timeLineViewModel.timeLineWidgetsStream.unsubscribe(this)
    }


}