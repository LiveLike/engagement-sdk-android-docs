import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.livelike_timeline_item.view.widget_view


class TimeLineViewAdapter(private val context: Context, private val sdk: EngagementSDK) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    val list: ArrayList<TimelineWidgetResource> = arrayListOf()
    var isLoadingAdded = false
    var isEndReached = false

    override fun onCreateViewHolder(p0: ViewGroup, viewtype: Int): RecyclerView.ViewHolder {
        return when (viewtype){

            VIEW_TYPE_DATA -> {
                TimeLineItemViewHolder(
                    LayoutInflater.from(p0.context).inflate(
                        R.layout.livelike_timeline_item,
                        p0,
                        false
                    )
                )
            }

            VIEW_TYPE_PROGRESS -> {
                ProgressViewHolder(
                    LayoutInflater.from(p0.context).inflate(
                        R.layout.livelike_progress_item,
                        p0,
                        false
                    )
                )
            }

            else -> throw IllegalArgumentException("Different View type")
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (position == list.size - 1 && isLoadingAdded && !isEndReached) VIEW_TYPE_PROGRESS else VIEW_TYPE_DATA
    }


    override fun onBindViewHolder(itemViewHolder: RecyclerView.ViewHolder, p1: Int) {
        if (itemViewHolder is TimeLineItemViewHolder){
            val timelineWidgetResource = list[p1]
            val liveLikeWidget = timelineWidgetResource.liveLikeWidget
            itemViewHolder.itemView.widget_view.enableDefaultWidgetTransition = false
            itemViewHolder.itemView.widget_view.displayWidget(
                sdk,
                liveLikeWidget
            )
            itemViewHolder.itemView.widget_view.setState(timelineWidgetResource.widgetState)
        }
    }

    override fun getItemCount(): Int = list.size


    override fun getItemId(position: Int): Long {
        return list[position].liveLikeWidget.id.hashCode().toLong()
    }


    companion object
    {
        private const val VIEW_TYPE_DATA = 0
        private const val VIEW_TYPE_PROGRESS = 1 // for load more // progress view type
    }

}

class TimeLineItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

class ProgressViewHolder(view: View): RecyclerView.ViewHolder(view)

data class TimelineWidgetResource(
    var widgetState: WidgetStates,
    val liveLikeWidget: LiveLikeWidget
)

