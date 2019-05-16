package com.livelike.livelikesdk.widget.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.widget.model.Option
import com.livelike.livelikesdk.widget.view.components.TextItemView

internal class BaseViewAdapter(private val myDataset: List<Option>) :
    RecyclerView.Adapter<BaseViewAdapter.TextOptionViewHolder>() {

    var selectedPosition = RecyclerView.NO_POSITION
    var selectionLocked = false
    var predictionSelected = ""
    var predictionCorrect = ""

    inner class TextOptionViewHolder(val textItemView: TextItemView, val onClick: (selectedOption: Option) -> Unit) :
        RecyclerView.ViewHolder(textItemView),
        View.OnClickListener {
        init {
            textItemView.clickListener = this
        }

        override fun onClick(v: View?) {
            if (adapterPosition == RecyclerView.NO_POSITION || selectionLocked) return

            // Updating old as well as new positions
            notifyItemChanged(selectedPosition)
            selectedPosition = adapterPosition
            notifyItemChanged(selectedPosition)

            onClick(myDataset[selectedPosition])
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TextOptionViewHolder {
        val textView = TextItemView(parent.context)
        return TextOptionViewHolder(textView)
    }

    override fun onBindViewHolder(holder: TextOptionViewHolder, position: Int) {
        val item = myDataset[position]
        holder.textItemView.setData(item, itemIsSelected)

        if (selectedPosition == position) {
            holder.textItemView.updateViewBackground(R.drawable.button_poll_answer_outline)
        } else {
            holder.textItemView.updateViewBackground(R.drawable.button_default)
        }

        // it's follow up
        if (predictionCorrect.isNotEmpty()) {
            if (predictionSelected == item.id) {
                holder.textItemView.updateViewBackground(R.drawable.button_wrong_answer_outline)
            }
            if (predictionCorrect == item.id) {
                holder.textItemView.updateViewBackground(R.drawable.button_correct_answer_outline)
            }
        }
        holder.textItemView.setProgressVisibility(predictionCorrect.isNotEmpty())
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}