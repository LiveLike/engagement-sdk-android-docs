package com.livelike.livelikesdk.widget.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Option
import com.livelike.livelikesdk.widget.view.components.WidgetItemView

internal class WidgetOptionsViewAdapter(
    internal var myDataset: List<Option>,
    private val onClick: (selectedOption: Option) -> Unit,
    private val widgetType: WidgetType,
    var correctOptionId: String = "",
    var userSelectedOptionId: String = ""
) :
    RecyclerView.Adapter<WidgetOptionsViewAdapter.TextOptionViewHolder>() {

    var selectedPosition = RecyclerView.NO_POSITION
    var selectionLocked = false
    var showPercentage = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class TextOptionViewHolder(val textItemView: WidgetItemView, val onClick: (selectedOption: Option) -> Unit) :
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
        val textView = WidgetItemView(parent.context)
        return TextOptionViewHolder(textView, onClick)
    }

    // TODO: Remove the logic from the adapter and move it to the view itself
    override fun onBindViewHolder(holder: TextOptionViewHolder, position: Int) {
        val item = myDataset[position]
        val itemIsSelected = selectedPosition == position

        holder.textItemView.setData(item, itemIsSelected, widgetType, correctOptionId, userSelectedOptionId)
        if (showPercentage) {
            holder.textItemView.setProgressVisibility(showPercentage)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}