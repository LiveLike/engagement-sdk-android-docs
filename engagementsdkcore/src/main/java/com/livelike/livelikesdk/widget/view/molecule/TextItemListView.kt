package com.livelike.livelikesdk.widget.view.molecule

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.livelike.livelikesdk.widget.model.Option
import com.livelike.livelikesdk.widget.view.atom.TextItemView
import kotlinx.android.synthetic.main.atom_widget_text_item.view.text_button

internal class TextViewAdapter(private val myDataset: List<Option>) :
    RecyclerView.Adapter<TextViewAdapter.TextOptionViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class TextOptionViewHolder(val textItemView: TextItemView) : RecyclerView.ViewHolder(textItemView)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TextOptionViewHolder {
        // create a new view
        val textView = TextItemView(parent.context)
        // set the view's size, margins, paddings and layout parameters
        return TextOptionViewHolder(textView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: TextOptionViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        val item = myDataset[position]
        holder.textItemView.text_button.text = item.description
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}