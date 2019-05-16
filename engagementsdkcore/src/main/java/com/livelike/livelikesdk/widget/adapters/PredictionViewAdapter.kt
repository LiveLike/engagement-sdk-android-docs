package com.livelike.livelikesdk.widget.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.livelike.livelikesdk.widget.model.Option
import com.livelike.livelikesdk.widget.view.components.TextItemView

internal class PredictionViewAdapter(
    private val myDataset: List<Option>,
    private val onClick: (selectedOption: Option) -> Unit
) :
    RecyclerView.Adapter<PredictionViewAdapter.TextOptionViewHolder>() {

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
        return TextOptionViewHolder(textView, onClick)
    }

    // TODO: Remove the logic from the adapter and move it to the view itself
    override fun onBindViewHolder(holder: TextOptionViewHolder, position: Int) {
        val item = myDataset[position]
        val itemIsSelected = selectedPosition == position

        holder.textItemView.setData(item, itemIsSelected)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}