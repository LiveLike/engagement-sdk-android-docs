package com.livelike.engagementsdk.widget.timeline

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.livelike.engagementsdk.*
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.databinding.LivelikeProgressItemBinding
import com.livelike.engagementsdk.databinding.LivelikeTimelineItemBinding
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.WidgetProvider
import com.livelike.engagementsdk.widget.viewModel.WidgetStates


internal class TimeLineViewAdapter(
    private val context: Context,
    private val sdk: EngagementSDK,
    private val timeLineViewModel: WidgetTimeLineViewModel
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var widgetViewFactory: LiveLikeWidgetViewFactory? = null
    val list: ArrayList<TimelineWidgetResource> = arrayListOf()
    var isLoadingInProgress = false
    var isEndReached = false
    var liveLikeEngagementTheme: LiveLikeEngagementTheme? = null
    var widgetTimerController: WidgetTimerController? = null
    var itemBinding: LivelikeTimelineItemBinding? = null
    var progressBinding:LivelikeProgressItemBinding? =null

    override fun onCreateViewHolder(p0: ViewGroup, viewtype: Int): RecyclerView.ViewHolder {
        return when (viewtype) {

            VIEW_TYPE_DATA -> {
                itemBinding = LivelikeTimelineItemBinding.inflate(LayoutInflater.from(p0.context), p0, false)
               /* TimeLineItemViewHolder(
                    LayoutInflater.from(p0.context).inflate(
                        R.layout.livelike_timeline_item,
                        p0,
                        false
                    )
                )*/
                TimeLineItemViewHolder(itemBinding!!)
            }

            VIEW_TYPE_PROGRESS -> {
                progressBinding =  LivelikeProgressItemBinding.inflate(LayoutInflater.from(p0.context), p0, false)
               /* ProgressViewHolder(
                    LayoutInflater.from(p0.context).inflate(
                        R.layout.livelike_progress_item,
                        p0,
                        false
                    )
                )*/
                ProgressViewHolder(progressBinding!!)
            }

            else -> throw IllegalArgumentException("Different View type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == list.size - 1 && isLoadingInProgress && !isEndReached) VIEW_TYPE_PROGRESS else VIEW_TYPE_DATA
    }

    override fun onBindViewHolder(itemViewHolder: RecyclerView.ViewHolder, p1: Int) {
        if (itemViewHolder is TimeLineItemViewHolder) {
            val timelineWidgetResource = list[p1]
            // val liveLikeWidget = timelineWidgetResource.liveLikeWidget
            liveLikeEngagementTheme?.let {
                itemBinding?.widgetView?.applyTheme(it)
            }
            itemBinding?.apply {
                widgetView.enableDefaultWidgetTransition = false
                widgetView.showTimer = widgetTimerController != null
                widgetView.showDismissButton = false
                widgetView.widgetViewFactory = widgetViewFactory
            }

            displayWidget(itemViewHolder, timelineWidgetResource)
            itemBinding?.widgetView?.setState(
                maxOf(
                    timelineWidgetResource.widgetState,
                    itemBinding?.widgetView?.getCurrentState() ?: WidgetStates.READY
                )
            )
        }
    }

    private fun displayWidget(
        itemViewHolder: RecyclerView.ViewHolder,
        timelineWidgetResource: TimelineWidgetResource
    ) {

        val liveLikeWidget = timelineWidgetResource.liveLikeWidget
        val widgetResourceJson =
            JsonParser.parseString(GsonBuilder().create().toJson(liveLikeWidget)).asJsonObject
        var widgetType = widgetResourceJson.get("kind").asString
        widgetType = if (widgetType.contains("follow-up")) {
            "$widgetType-updated"
        } else {
            "$widgetType-created"
        }
        val widgetId = widgetResourceJson["id"].asString
        itemBinding?.widgetView?.run {
            // TODO segregate widget view and viewmodel creation
            val widgetView = WidgetProvider()
                .get(
                    null,
                    WidgetInfos(widgetType, widgetResourceJson, widgetId),
                    context,
                    sdk.analyticService.latest() ?: MockAnalyticsService(),
                    sdk.configurationStream.latest()!!,
                    {
                        widgetContainerViewModel?.currentWidgetViewStream?.onNext(null)
                    },
                    sdk.userRepository,
                    null,
                    SubscriptionManager(),
                    widgetViewThemeAttributes,
                    engagementSDKTheme,
                    (timeLineViewModel.contentSession as ContentSession).widgetInteractionRepository
                )
            timeLineViewModel.widgetViewModelCache[widgetId]?.let {
                widgetView?.widgetViewModel = it
            }
            timeLineViewModel.widgetViewModelCache[widgetId] = widgetView?.widgetViewModel
            widgetView?.widgetViewModel?.showDismissButton = false
            widgetView?.let { view ->
                displayWidget(widgetType, view)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    override fun getItemId(position: Int): Long {
        return list[position].liveLikeWidget.id.hashCode().toLong()
    }

    companion object {
        private const val VIEW_TYPE_DATA = 0
        private const val VIEW_TYPE_PROGRESS = 1 // for load more // progress view type
    }
}

class TimeLineItemViewHolder(val itemBinding: LivelikeTimelineItemBinding) : RecyclerView.ViewHolder(itemBinding.root)

class ProgressViewHolder(val progressItemBinding: LivelikeProgressItemBinding) : RecyclerView.ViewHolder(progressItemBinding.root)

data class TimelineWidgetResource(
    var widgetState: WidgetStates,
    val liveLikeWidget: LiveLikeWidget,
    var apiSource: WidgetApiSource // this has been added to show/hide animation . if real time widget animation will be shown else not
)
