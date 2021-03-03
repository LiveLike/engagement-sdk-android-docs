import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.FrameLayout
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.timeline.WidgetApiSource
import com.livelike.engagementsdk.widget.timeline.WidgetTimeLineViewModel
import kotlinx.android.synthetic.main.livelike_timeline_view.view.loadingSpinnerTimeline
import kotlinx.android.synthetic.main.livelike_timeline_view.view.timeline_snap_live
import kotlinx.android.synthetic.main.livelike_timeline_view.view.timeline_rv
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WidgetsTimeLineView(
    context: Context,
    private val timeLineViewModel: WidgetTimeLineViewModel,
    sdk: EngagementSDK
) : FrameLayout(context) {

    private var adapter: TimeLineViewAdapter
    private var snapToLiveAnimation: AnimatorSet? = null
    private var showingSnapToLive: Boolean = false
    private var isFirstItemVisible = false
    private var autoScrollTimeline = false

    // The minimum amount of items to have below your current scroll position
    // before loading more.
    private val visibleThreshold = 2



    init {
        inflate(context, R.layout.livelike_timeline_view, this)
        showLoadingSpinnerForTimeline()
        adapter =
            TimeLineViewAdapter(
                context,
                sdk
            )
        adapter.list.addAll(timeLineViewModel.timeLineWidgets)
        timeline_rv.layoutManager = LinearLayoutManager(context)
        timeline_rv.adapter = adapter
        initListeners()
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
        timeLineViewModel.timeLineWidgetsStream.subscribe(this) { pair ->
            pair?.let {
                if (pair.first == WidgetApiSource.REALTIME_API) {
                    adapter.list.addAll(0, pair.second)
                    adapter.notifyItemInserted(0)
                    adapter.isLoadingAdded = false
                } else {
                    adapter.list.addAll(pair.second)
                    adapter.notifyItemRangeInserted(
                        adapter.itemCount - pair.second.size,
                        adapter.itemCount
                    )
                    adapter.isLoadingAdded = false
                }
            }
        }
    }

    /**
     * view click listeners
     * snap to live added
     **/
    private fun initListeners(){
        val lm = timeline_rv.layoutManager as LinearLayoutManager
        timeline_rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(
                rv: RecyclerView,
                dx: Int,
                dy: Int
            ) {
                val firstVisible = lm.findFirstVisibleItemPosition()
                val topHasBeenReached = firstVisible == 0
                if (!autoScrollTimeline)
                    isFirstItemVisible = if (topHasBeenReached) {
                        hideSnapToLiveForWidgets()
                        true
                    } else {
                        showSnapToLiveForWidgets()
                        false
                    }
                if (topHasBeenReached) {
                    autoScrollTimeline = false
                }

                /**
                 * load more on scrolled (pagination)
                 **/
                if (!adapter.isLoadingAdded && !adapter.isEndReached) {
                    val totalItemCount = lm.itemCount
                    val lastVisibleItem = lm.findLastVisibleItemPosition()
                    if (totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                        timeLineViewModel.loadMore()
                        adapter.isLoadingAdded = true
                    }
                }
            }
        })

        timeline_snap_live.setOnClickListener {
            autoScrollTimeline = true
            snapToLiveForTimeline()
        }

            timeLineViewModel.widgetEventStream.subscribe(javaClass.simpleName) {
                logDebug { "Widget timeline event stream : $it" }
                when (it) {

                    WidgetTimeLineViewModel.WIDGET_LOADING_COMPLETE -> {
                        timeLineViewModel.uiScope.launch {
                            // Added delay to avoid UI glitch when recycler view is loading
                            delay(500)
                            hideLoadingSpinnerForTimeline()

                        }
                    }

                    WidgetTimeLineViewModel.WIDGET_TIMELINE_END -> {
                        timeLineViewModel.uiScope.launch {
                            // Added delay to avoid UI glitch when recycler view is loading
                            delay(300)
                            adapter.isLoadingAdded = true
                            adapter.isEndReached = true
                            adapter.notifyItemChanged(adapter.list.size - 1)
                        }
                    }

                    WidgetTimeLineViewModel.WIDGET_LOADING_STARTED -> {
                        timeLineViewModel.uiScope.launch {
                            delay(400)
                            showLoadingSpinnerForTimeline()
                        }
                    }

                }
            }
    }


    private fun showLoadingSpinnerForTimeline() {
        loadingSpinnerTimeline.visibility = View.VISIBLE
        timeline_rv.visibility = View.GONE
        timeline_snap_live.visibility = View.GONE
    }

    private fun hideLoadingSpinnerForTimeline() {
        loadingSpinnerTimeline.visibility = View.GONE
        timeline_rv.visibility = View.VISIBLE
    }



    /**
     * used for hiding the Snap to live button
     * snap to live is mainly responsible for showing user the latest widget
     * if user is already at the latest widget,then usually this icon remain hidden
     **/
    private fun hideSnapToLiveForWidgets() {
        logDebug { "Widget Timeline hide Snap to Live: $showingSnapToLive" }
        if (!showingSnapToLive)
            return
        showingSnapToLive = false
        timeline_snap_live.visibility = View.GONE
        animateSnapToLiveButton()
    }


    /**
     * used for showing the Snap to Live button
     **/
    private fun showSnapToLiveForWidgets() {
        logDebug { "Wdget Timeline show Snap to Live: $showingSnapToLive" }
        if (showingSnapToLive)
            return
        showingSnapToLive = true
        timeline_snap_live.visibility = View.VISIBLE
        animateSnapToLiveButton()
    }



    private fun snapToLiveForTimeline() {
        timeline_rv?.let { rv ->
            hideSnapToLiveForWidgets()
            timeLineViewModel.timeLineWidgets?.size?.let {
                timeline_rv.postDelayed({
                    rv.smoothScrollToPosition(0)
                }, 200)
            }
        }
    }


    private fun animateSnapToLiveButton() {
        snapToLiveAnimation?.cancel()

        val translateAnimation = ObjectAnimator.ofFloat(
            timeline_snap_live,
            "translationY",
            if (showingSnapToLive) 0f else AndroidResource.dpToPx(TIMELINE_SNAP_TO_LIVE_ANIMATION_DESTINATION)
                .toFloat()
        )
        translateAnimation?.duration = TIMELINE_SNAP_TO_LIVE_ANIMATION_DURATION.toLong()
        val alphaAnimation =
            ObjectAnimator.ofFloat(timeline_snap_live, "alpha", if (showingSnapToLive) 1f else 0f)
        alphaAnimation.duration = (TIMELINE_SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION).toLong()
        alphaAnimation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                timeline_snap_live.visibility = if (showingSnapToLive) View.VISIBLE else View.GONE
            }

            override fun onAnimationStart(animation: Animator) {
                timeline_snap_live.visibility = if (showingSnapToLive) View.GONE else View.VISIBLE
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        snapToLiveAnimation = AnimatorSet()
        snapToLiveAnimation?.play(translateAnimation)?.with(alphaAnimation)
        snapToLiveAnimation?.start()
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unsubscribeForTimelineWidgets()
    }


    private fun unsubscribeForTimelineWidgets() {
        timeLineViewModel.timeLineWidgetsStream.unsubscribe(this)
        timeLineViewModel.widgetEventStream.unsubscribe(this)
    }

    companion object {
        const val TIMELINE_SNAP_TO_LIVE_ANIMATION_DURATION = 400F
        const val TIMELINE_SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION = 320F
        const val TIMELINE_SNAP_TO_LIVE_ANIMATION_DESTINATION = 50

    }


}