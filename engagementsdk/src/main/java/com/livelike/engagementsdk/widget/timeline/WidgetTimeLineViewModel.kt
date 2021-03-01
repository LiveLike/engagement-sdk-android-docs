package com.livelike.engagementsdk.widget.timeline

import TimelineWidgetResource
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WidgetTimeLineViewModel(private val contentSession: LiveLikeContentSession) : ViewModel() {

    /**
     * it contains all the widgets that has been loaded onto this timeline
     **/
    val timeLineWidgets = mutableListOf<TimelineWidgetResource>()

    val timeLineWidgetsStream : Stream<List<TimelineWidgetResource>> = SubscriptionManager(false)

    var decideWidgetInteractivity: DecideWidgetInteractivity? = null

    init {
       loadPastPublishedWidgets(LiveLikePagination.FIRST)
    }


    private fun loadPastPublishedWidgets(page: LiveLikePagination) {
        contentSession.getPublishedWidgets(
            page,
            object : LiveLikeCallback<List<LiveLikeWidget>>() {
                override fun onResponse(result: List<LiveLikeWidget>?, error: String?) {
                    result?.let { list ->
                        val widgets = list.map {
                            TimelineWidgetResource(
                                decideWidgetInteraction(it, TimeLineWidgetApiSource.HISTORY_API),
                                it
                            )
                        }
                        timeLineWidgets.addAll(widgets)
                        uiScope.launch {
                            timeLineWidgetsStream.onNext(widgets)
                        }
                    }
                }
            })
    }

    /**
     * this call load the next available page of past published widgets on this program.
     **/
    fun loadMore(){
        loadPastPublishedWidgets(LiveLikePagination.NEXT)
    }

    private fun observeForLiveWidgets() {
        contentSession.widgetStream.subscribe(this) {
            it?.let {
                val widget =  TimelineWidgetResource(
                    decideWidgetInteraction(it, TimeLineWidgetApiSource.REALTIME_API),
                    it
                )
               uiScope.launch {
                   timeLineWidgetsStream.onNext(mutableListOf(widget))
               }
            }
        }
    }


    fun wouldAllowWidgetInteraction(liveLikeWidget: LiveLikeWidget) : Boolean{
        return timeLineWidgets.find { it.liveLikeWidget.id == liveLikeWidget.id }?.
                    widgetState == WidgetStates.INTERACTING
    }

    private fun decideWidgetInteraction(liveLikeWidget: LiveLikeWidget, timeLineWidgetApiSource: TimeLineWidgetApiSource ) : WidgetStates{
        var isInteractive = false
        isInteractive = if(decideWidgetInteractivity!=null){
            decideWidgetInteractivity?.wouldAllowWidgetInteraction(liveLikeWidget)?:false
        }else{
            timeLineWidgetApiSource == TimeLineWidgetApiSource.REALTIME_API
        }
        return if (isInteractive) WidgetStates.INTERACTING else WidgetStates.RESULTS
    }

    /**
     * Call this method to close down all connections and scopes, it should be called from onClear() method
     * of android viewmodel. In case landscape not supported then onDestroy.
     */
    fun clear(){
        uiScope.cancel()
        contentSession.widgetStream.unsubscribe(this)
    }

}



// Timeline view will have default implementation
interface DecideWidgetInteractivity {
//    TODO discuss with team if there is a requirement to add TimeLineWidgetApiSource as param in this function
    fun wouldAllowWidgetInteraction(widget : LiveLikeWidget) : Boolean
}

enum class TimeLineWidgetApiSource{
    REALTIME_API,
    HISTORY_API
}
