package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.OptionsItem
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.PollWidgetUserInteraction
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_poll_widget.view.button2
import kotlinx.android.synthetic.main.custom_poll_widget.view.imageView2
import kotlinx.android.synthetic.main.custom_poll_widget.view.rcyl_poll_list
import kotlinx.android.synthetic.main.quiz_image_list_item.view.imageButton2
import kotlinx.android.synthetic.main.quiz_image_list_item.view.textView8
import kotlinx.android.synthetic.main.quiz_list_item.view.button4
import kotlinx.android.synthetic.main.quiz_list_item.view.textView7

class CustomPollWidget : ConstraintLayout {
    var pollWidgetModel: PollWidgetModel? = null
    var isImage = false

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        inflate(context, R.layout.custom_poll_widget, this@CustomPollWidget)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // this is added just to test exposed api loadWidgetInteraction
        pollWidgetModel?.loadWidgetInteraction( object : LiveLikeCallback<List<PollWidgetUserInteraction>>(){
            override fun onResponse(result: List<PollWidgetUserInteraction>?, error: String?) {
               if(result!=null){
                   if(result.isNotEmpty()){
                       Log.d("interaction-poll",result[0].optionId)
                   }
               }
            }
        })

        pollWidgetModel?.widgetData?.let { liveLikeWidget ->
            liveLikeWidget.options?.let {
                if (it.size > 2) {
                    rcyl_poll_list.layoutManager =
                        GridLayoutManager(
                            context,
                            2
                        )
                }
                val adapter =
                    PollListAdapter(context, isImage, ArrayList(it.map { item -> item!! }))
                rcyl_poll_list.adapter = adapter
                adapter.pollListener = object : PollListAdapter.PollListener {
                    override fun onSelectOption(id: String) {
                        pollWidgetModel?.submitVote(id)
                    }
                }
                button2.visibility = View.GONE
                pollWidgetModel?.voteResults?.subscribe(this) { result ->
                    result?.choices?.let { options ->
                        options.forEach { op ->
                            adapter.optionIdCount[op.id] = op.vote_count ?: 0
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            imageView2.setOnClickListener {
                pollWidgetModel?.finish()
            }

        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pollWidgetModel?.voteResults?.unsubscribe(this)
    }
}

class PollListAdapter(
    private val context: Context,
    private val isImage: Boolean,
    val list: ArrayList<OptionsItem>
) :
    RecyclerView.Adapter<PollListAdapter.PollListItemViewHolder>() {
    var selectedIndex = -1
    val optionIdCount: HashMap<String, Int> = hashMapOf()

    var isFollowUp = false

    fun getSelectedOption(): OptionsItem? = when (selectedIndex > -1) {
        true -> list[selectedIndex]
        else -> null
    }

    var pollListener: PollListener? = null

    interface PollListener {
        fun onSelectOption(id: String)
    }

    class PollListItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): PollListItemViewHolder {
        return PollListItemViewHolder(
            LayoutInflater.from(p0.context!!).inflate(
                when (isImage) {
                    true -> R.layout.quiz_image_list_item
                    else -> R.layout.quiz_list_item
                }, p0, false
            )
        )
    }

    override fun onBindViewHolder(holder: PollListItemViewHolder, index: Int) {
        val item = list[index]
        if (isImage) {
            Glide.with(context)
                .load(item.imageUrl)
                .into(holder.itemView.imageButton2)
            if (selectedIndex == index) {
                holder.itemView.imageButton2.setBackgroundColor(Color.BLUE)
            } else {
                holder.itemView.imageButton2.setBackgroundColor(Color.GRAY)
            }
            holder.itemView.textView8.text = "${optionIdCount[item.id!!] ?: 0}"

            holder.itemView.imageButton2.setOnClickListener {
                selectedIndex = holder.adapterPosition
                pollListener?.onSelectOption(item.id!!)
                notifyDataSetChanged()
            }
        } else {
            holder.itemView.textView7.text = "${optionIdCount[item.id!!] ?: 0}"
            holder.itemView.button4.text = "${item.description}"
            if (selectedIndex == index) {
                holder.itemView.button4.setBackgroundColor(Color.BLUE)
            } else {
                holder.itemView.button4.setBackgroundColor(Color.GRAY)
            }

            holder.itemView.button4.setOnClickListener {
                if(!isFollowUp) {
                    selectedIndex = holder.adapterPosition
                    pollListener?.onSelectOption(item.id!!)
                    notifyDataSetChanged()
                }
            }
        }

    }

    override fun getItemCount(): Int = list.size
}
