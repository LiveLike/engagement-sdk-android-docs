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
import com.livelike.engagementsdk.widget.timeline.WidgetApiSource
import com.livelike.engagementsdk.widget.timeline.WidgetTimeLineViewModel
import kotlinx.android.synthetic.main.livelike_timeline_view.view.loadingSpinner
import kotlinx.android.synthetic.main.livelike_timeline_view.view.snap_live
import kotlinx.android.synthetic.main.livelike_timeline_view.view.timeline_rv

class WidgetsTimeLineView(
    context: Context,
    private val timeLineViewModel: WidgetTimeLineViewModel,
    sdk: EngagementSDK
) : FrameLayout(context) {

    private var adapter: TimeLineViewAdapter
    private var snapToLiveAnimation: AnimatorSet? = null
    private var showingSnapToLive: Boolean = false
    private var isFirstItemVisible = false
    private var autoScroll = false


    init {
        inflate(context, R.layout.livelike_timeline_view, this)
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
                } else {
                    adapter.list.addAll(pair.second)
                    adapter.notifyItemRangeInserted(
                        adapter.itemCount - pair.second.size,
                        adapter.itemCount
                    )
                }
            }
        }
    }

    /**
     * view click listeners
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

                val topHasBeenReached = firstVisible - 4 <= 0
                if (!autoScroll)
                    isFirstItemVisible = if (topHasBeenReached) {
                        hideSnapToLive()
                        true
                    } else {
                        showSnapToLive()
                        false
                    }
                if (topHasBeenReached) {
                    autoScroll = false
                }
            }
        })

        snap_live.setOnClickListener {
            autoScroll = true
            snapToLive()
        }
    }


    private fun showLoadingSpinner() {
        loadingSpinner.visibility = View.VISIBLE
        timeline_rv.visibility = View.GONE
        snap_live.visibility = View.GONE
    }

    private fun hideLoadingSpinner() {
        loadingSpinner.visibility = View.GONE
        timeline_rv.visibility = View.VISIBLE
    }



    /**
     * used for hiding the Snap to live button
     * snap to live is mainly responsible for showing user the latest widget received
     * if user is already at the latest widget,then usually this icon remain hidden
     **/
    private fun hideSnapToLive() {
        logDebug { "Widget Timeline hide Snap to Live: $showingSnapToLive" }
        if (!showingSnapToLive)
            return
        showingSnapToLive = false
        snap_live.visibility = View.GONE
        animateSnapToLiveButton()
    }


    /**
     * used for showing the Snap to Live button
     **/
    private fun showSnapToLive() {
        logDebug { "Wdget Timeline show Snap to Live: $showingSnapToLive" }
        if (showingSnapToLive)
            return
        showingSnapToLive = true
        snap_live.visibility = View.VISIBLE
        animateSnapToLiveButton()
    }



    private fun snapToLive() {
        timeline_rv?.let { rv ->
            hideSnapToLive()
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
            snap_live,
            "translationY",
            if (showingSnapToLive) 0f else AndroidResource.dpToPx( SNAP_TO_LIVE_ANIMATION_DESTINATION)
                .toFloat()
        )
        translateAnimation?.duration = SNAP_TO_LIVE_ANIMATION_DURATION.toLong()
        val alphaAnimation =
            ObjectAnimator.ofFloat(snap_live, "alpha", if (showingSnapToLive) 1f else 0f)
        alphaAnimation.duration = (SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION).toLong()
        alphaAnimation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                snap_live.visibility = if (showingSnapToLive) View.VISIBLE else View.GONE
            }

            override fun onAnimationStart(animation: Animator) {
                snap_live.visibility = if (showingSnapToLive) View.GONE else View.VISIBLE
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
    }

    companion object {
        const val SNAP_TO_LIVE_ANIMATION_DURATION = 400F
        const val SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION = 320F
        const val SNAP_TO_LIVE_ANIMATION_DESTINATION = 50
    }


}