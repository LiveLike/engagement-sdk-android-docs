
import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.livelike.engagementsdk.OptionsItem
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.livelike.livelikedemo.R
import com.livelike.livelikedemo.customwidgets.parseDuration
import kotlinx.android.synthetic.main.mml_poll_image_list_item.view.imageView
import kotlinx.android.synthetic.main.mml_poll_image_list_item.view.lay_poll_img_option
import kotlinx.android.synthetic.main.mml_poll_image_list_item.view.progressBar
import kotlinx.android.synthetic.main.mml_poll_image_list_item.view.textView
import kotlinx.android.synthetic.main.mml_poll_image_list_item.view.textView2
import kotlinx.android.synthetic.main.mml_poll_text_list_item.view.lay_poll_text_option
import kotlinx.android.synthetic.main.mml_poll_text_list_item.view.progressBar_text
import kotlinx.android.synthetic.main.mml_poll_text_list_item.view.text_poll_item
import kotlinx.android.synthetic.main.mml_poll_text_list_item.view.txt_percent
import kotlinx.android.synthetic.main.mml_poll_widget.view.rcyl_poll_list
import kotlinx.android.synthetic.main.mml_poll_widget.view.time_bar
import kotlinx.android.synthetic.main.mml_poll_widget.view.txt_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.util.HashMap
import kotlin.collections.set

class MMLPollWidget(
    context: Context,
    private var pollWidgetModel: PollWidgetModel,
    var timelineWidgetResource: TimelineWidgetResource? = null,
    private var isImage: Boolean = false,
    private val isActive : Boolean = false
) : ConstraintLayout(context) {
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    init {
        inflate(context, R.layout.mml_poll_widget, this)
        initView()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    private fun initView() {
        pollWidgetModel.widgetData.let { liveLikeWidget ->
            txt_title.text = liveLikeWidget.question
            liveLikeWidget.options?.let { list ->
                if (isImage) {
                    rcyl_poll_list.layoutManager =
                        GridLayoutManager(
                            context,
                            2
                        )
                } else {
                    rcyl_poll_list.layoutManager =
                        LinearLayoutManager(
                            context,
                            RecyclerView.VERTICAL,
                            false
                        )
                }
                val adapter =
                    PollListAdapter(
                        context,
                        isImage,
                        ArrayList(list.map { item -> item!! })
                    )

                rcyl_poll_list.adapter = adapter
                if (!isActive) {
                    adapter.isTimeLine = true
                    list.forEach { op ->
                        op?.let {
                            adapter.optionIdCount[op.id!!] = op.voteCount ?: 0
                        }
                    }

                    adapter.notifyDataSetChanged()
                    time_bar.visibility = View.INVISIBLE
                } else {
                    adapter.pollListener = object : PollListAdapter.PollListener {
                        override fun onSelectOption(optionsItem: OptionsItem) {
                                optionsItem.id?.let {
                                    pollWidgetModel.submitVote(it)
                                }
                        }
                    }
                    adapter.notifyDataSetChanged()
                    pollWidgetModel.voteResults.subscribe(this@MMLPollWidget) { result ->
                        result?.choices?.let { options ->
                            var change = false
                            options.forEach { op ->
                                if (!adapter.optionIdCount.containsKey(op.id) || adapter.optionIdCount[op.id] != op.vote_count) {
                                    change = true
                                }
                                adapter.optionIdCount[op.id] = op.vote_count ?: 0
                            }
                            if (change)
                                adapter.notifyDataSetChanged()
                        }
                    }

                    val timeMillis = liveLikeWidget.timeout?.parseDuration() ?: 5000
                    time_bar.visibility = View.VISIBLE
                    time_bar.startTimer(timeMillis, timeMillis)

                    uiScope.async {
                        delay(timeMillis)
                        timelineWidgetResource?.widgetState = WidgetStates.RESULTS
                        adapter.isTimeLine = true
                        adapter.notifyDataSetChanged()
                        pollWidgetModel.voteResults.unsubscribe(this@MMLPollWidget)
                        if (timelineWidgetResource == null) {
                            pollWidgetModel.finish()
                        } else {
                            time_bar.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

}

class PollListAdapter(
    private val context: Context,
    private val isImage: Boolean,
    private val list: ArrayList<OptionsItem>
) :
    RecyclerView.Adapter<PollListAdapter.PollListItemViewHolder>() {
    var selectedIndex = -1
    val optionIdCount: HashMap<String, Int> = hashMapOf()
    var pollListener: PollListener? = null
    var isTimeLine: Boolean = false

    interface PollListener {
        fun onSelectOption(optionsItem: OptionsItem)
    }

    class PollListItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): PollListAdapter.PollListItemViewHolder {
        return PollListAdapter.PollListItemViewHolder(
            LayoutInflater.from(p0.context!!).inflate(
                when (isImage) {
                    true -> R.layout.mml_poll_image_list_item
                    else -> R.layout.mml_poll_text_list_item
                }, p0, false
            )
        )
    }


    override fun onBindViewHolder(holder: PollListAdapter.PollListItemViewHolder, index: Int) {
        val item = list[index]
        if (isImage) {
            Glide.with(context)
                .load(item.imageUrl)
                .transform(RoundedCorners(holder.itemView.resources.getDimensionPixelSize(R.dimen.rounded_corner_box_radius)))
                .into(holder.itemView.imageView)
            if (optionIdCount.containsKey(item.id)) {
                holder.itemView.progressBar.visibility = View.VISIBLE
                holder.itemView.textView2.visibility = View.VISIBLE
                 val total = optionIdCount.values.reduce { acc, i -> acc + i }
                val percent = when (total > 0) {
                    true -> (optionIdCount[item.id!!]!!.toFloat() / total.toFloat()) * 100
                    else -> 0F
                }
                holder.itemView.progressBar.progress = percent.toInt()
                holder.itemView.textView2.text = "${percent.toInt()} %"
            } else {
                holder.itemView.progressBar.visibility = View.INVISIBLE
                holder.itemView.textView2.visibility = View.GONE
            }
            holder.itemView.textView.text = "${item.description}"
            if (selectedIndex == index) {
                holder.itemView.lay_poll_img_option.setBackgroundResource(R.drawable.mml_image_option_background_selected_drawable)
                holder.itemView.progressBar.progressDrawable = ContextCompat.getDrawable(
                    context,
                    R.drawable.mml_custom_progress_color_options_selected
                )
            } else {
                holder.itemView.lay_poll_img_option.setBackgroundResource(R.drawable.mml_image_option_background_stroke_drawable)
                holder.itemView.progressBar.progressDrawable = ContextCompat.getDrawable(
                    context,
                    R.drawable.mml_custom_progress_color_options
                )
            }
            if (!isTimeLine)
                holder.itemView.lay_poll_img_option.setOnClickListener {
                    selectedIndex = holder.adapterPosition
                    pollListener?.onSelectOption(item)
                    notifyDataSetChanged()
                }
            else
                holder.itemView.lay_poll_img_option.setOnClickListener(null)
        } else {
            if (optionIdCount.containsKey(item.id)) {
                holder.itemView.txt_percent.visibility = View.VISIBLE
                holder.itemView.progressBar_text.visibility = View.VISIBLE
                val total = optionIdCount.values.reduce { acc, i -> acc + i }
                val percent = when (total > 0) {
                    true -> (optionIdCount[item.id!!]!!.toFloat() / total.toFloat()) * 100
                    else -> 0F
                }
                holder.itemView.txt_percent.text = "${percent.toInt()} %"
                holder.itemView.progressBar_text.progress = percent.toInt()
            } else {
                holder.itemView.txt_percent.visibility = View.INVISIBLE
                holder.itemView.progressBar_text.visibility = View.INVISIBLE
            }
            holder.itemView.text_poll_item.text = "${item.description}"
            if (selectedIndex == index) {
                holder.itemView.lay_poll_text_option.setBackgroundResource(R.drawable.mml_image_option_background_selected_drawable)
                holder.itemView.progressBar_text.progressDrawable = ContextCompat.getDrawable(
                    context,
                    R.drawable.mml_custom_progress_color_options_selected
                )
            } else {
                holder.itemView.lay_poll_text_option.setBackgroundResource(R.drawable.mml_image_option_background_stroke_drawable)
                holder.itemView.progressBar_text.progressDrawable = ContextCompat.getDrawable(
                    context,
                    R.drawable.mml_custom_progress_color_options
                )
            }
            if (!isTimeLine)
                holder.itemView.lay_poll_text_option.setOnClickListener {
                    selectedIndex = holder.adapterPosition
                    pollListener?.onSelectOption(item)
                    notifyDataSetChanged()
                }
            else
                holder.itemView.lay_poll_text_option.setOnClickListener(null)
        }

    }

    override fun getItemCount(): Int = list.size

}


