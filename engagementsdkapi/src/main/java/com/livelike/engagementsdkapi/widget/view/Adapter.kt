package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.widget.model.VoteOption
import kotlinx.android.synthetic.main.prediction_image_row_element.view.*

// TODO: This adapter can be made generic and used in most result widgets.
class Adapter(
    private val optionList: List<VoteOption>,
    private val optionSelectedCallback: (String?) -> Unit,
    private var correctOption: String?,
    private val context: Context,
    private val viewOptions : MutableMap<String?, ViewOption>,
    private val imageButtonMap : HashMap<ImageButton, String?>,
    var selectedOption: String? = null,
    var userTapped: () -> Unit
) : RecyclerView.Adapter<ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = optionList[position]
        holder.optionText.text = option.description
        option.isCorrect?.let { if(it) correctOption = option.id }

        // TODO: Move this to adapter layer.
        Glide.with(context)
            .load(option.imageUrl)
            .into(holder.optionButton)
        imageButtonMap[holder.optionButton] = option.id
        holder.optionButton.setOnClickListener {
            selectedOption = imageButtonMap[holder.optionButton]
            optionSelectedCallback(selectedOption)
            userTapped.invoke()
        }
        viewOptions[option.id] = ViewOption(holder.optionButton, holder.progressBar, holder.percentageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.prediction_image_row_element,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return optionList.size
    }
}
class ViewOption(
    val button: ImageButton,
    val progressBar: ProgressBar,
    val percentageTextView: TextView
)

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val optionButton: ImageButton = view.image_button
    val optionText: TextView = view.item_text
    val percentageText: TextView = view.result_percentage_text
    val progressBar: ProgressBar = view.determinateBar
}