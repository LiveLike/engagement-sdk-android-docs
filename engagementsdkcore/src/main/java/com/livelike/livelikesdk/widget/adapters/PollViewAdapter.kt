package com.livelike.livelikesdk.widget.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.services.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.widget.WidgetDataClient
import com.livelike.livelikesdk.widget.model.Option
import com.livelike.livelikesdk.widget.view.components.TextItemView

internal class PollViewAdapter(private val myDataset: List<Option>) :
    RecyclerView.Adapter<PollViewAdapter.TextOptionViewHolder>() {

    var selectedPosition = RecyclerView.NO_POSITION
    var selectionLocked = false
    var predictionSelected = ""
    var predictionCorrect = ""

    inner class TextOptionViewHolder(val textItemView: TextItemView) : RecyclerView.ViewHolder(textItemView),
        View.OnClickListener {
        init {
            textItemView.clickListener = this
        }

        override fun onClick(v: View?) {
            if (adapterPosition == RecyclerView.NO_POSITION || selectionLocked) return
            notifyItemChanged(selectedPosition)
            selectedPosition = adapterPosition
            notifyItemChanged(selectedPosition)
            myDataset[selectedPosition].getMergedVoteUrl()
                ?.let { dataClient.vote(it) } // TODO: this needs to be debounced
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TextOptionViewHolder {
        val textView = TextItemView(parent.context)
        return TextOptionViewHolder(textView)
    }

    private val dataClient: WidgetDataClient = LiveLikeDataClientImpl()

    override fun onBindViewHolder(holder: TextOptionViewHolder, position: Int) {
        val item = myDataset[position]
        holder.textItemView.setData(item)

        if (selectedPosition == position) {
            holder.textItemView.updateViewBackground(R.drawable.button_poll_answer_outline)
        } else {
            holder.textItemView.updateViewBackground(R.drawable.button_default)
        }

        // for poll this would be when time expires
        if (predictionCorrect.isNotEmpty()) {
            if (predictionSelected == item.id) {
                holder.textItemView.updateViewBackground(R.drawable.button_wrong_answer_outline)
            }
            if (predictionCorrect == item.id) {
                holder.textItemView.updateViewBackground(R.drawable.button_correct_answer_outline)
            }
        }

        // for poll this is set to visible on click
        holder.textItemView.setProgressVisibility(selectedPosition != RecyclerView.NO_POSITION)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}