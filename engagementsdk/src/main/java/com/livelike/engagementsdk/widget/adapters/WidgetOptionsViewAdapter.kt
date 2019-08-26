package com.livelike.engagementsdk.widget.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Option
import com.livelike.engagementsdk.widget.view.components.WidgetItemView

internal class WidgetOptionsViewAdapter(
    internal var myDataset: List<Option>,
    private val onClick: (selectedOption: Option) -> Unit,
    private val widgetType: WidgetType,
    var correctOptionId: String? = null,
    var userSelectedOptionId: String = ""
) :
    RecyclerView.Adapter<WidgetOptionsViewAdapter.TextOptionViewHolder>() {

    var selectedPosition = RecyclerView.NO_POSITION
    var selectionLocked = false
    var showPercentage = false
        set(value) {
            if (field != value && value) {
                notifyDataSetChanged()
            }
            field = value
        }

    inner class TextOptionViewHolder(val textItemView: WidgetItemView, val onClick: (selectedOption: Option) -> Unit) :
        RecyclerView.ViewHolder(textItemView),
        View.OnClickListener {
        init {
            textItemView.clickListener = this
        }

        override fun onClick(v: View?) {
            if (adapterPosition == RecyclerView.NO_POSITION || selectionLocked || selectedPosition == adapterPosition) return

            selectedPosition = adapterPosition
            notifyDataSetChanged()

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

    override fun onBindViewHolder(holder: TextOptionViewHolder, position: Int) {
        val item = myDataset[position]
        val itemIsSelected = selectedPosition == position
        val itemIsLast = myDataset.size - 1 == position

        holder.textItemView.setData(item, itemIsSelected, widgetType, correctOptionId, userSelectedOptionId, itemIsLast)
        if (showPercentage) {
            holder.textItemView.setProgressVisibility(showPercentage)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}
