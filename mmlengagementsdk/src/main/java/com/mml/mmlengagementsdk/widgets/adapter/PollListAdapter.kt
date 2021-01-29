package com.mml.mmlengagementsdk.widgets.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.OptionsItem
import com.mml.mmlengagementsdk.widgets.utils.setCustomFontWithTextStyle
import kotlinx.android.synthetic.main.mml_poll_image_list_item.view.imageView
import kotlinx.android.synthetic.main.mml_poll_image_list_item.view.lay_poll_img_option
import kotlinx.android.synthetic.main.mml_poll_image_list_item.view.progressBar
import kotlinx.android.synthetic.main.mml_poll_image_list_item.view.textView
import kotlinx.android.synthetic.main.mml_poll_image_list_item.view.textView2
import kotlinx.android.synthetic.main.mml_poll_text_list_item.view.lay_poll_text_option
import kotlinx.android.synthetic.main.mml_poll_text_list_item.view.progressBar_text
import kotlinx.android.synthetic.main.mml_poll_text_list_item.view.text_poll_item
import kotlinx.android.synthetic.main.mml_poll_text_list_item.view.txt_percent

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
        fun onSelectOption(id: String)
    }

    class PollListItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): PollListItemViewHolder {
        return PollListItemViewHolder(
            LayoutInflater.from(p0.context!!).inflate(
                when (isImage) {
                    true -> R.layout.mml_poll_image_list_item
                    else -> R.layout.mml_poll_text_list_item
                }, p0, false
            )
        )
    }

    override fun onBindViewHolder(holder: PollListItemViewHolder, index: Int) {
        val item = list[index]
        if (isImage) {
            Glide.with(context)
                .load(item.imageUrl)
                .transform(RoundedCorners(holder.itemView.resources.getDimensionPixelSize(R.dimen.rounded_corner_box_radius)))
                .into(holder.itemView.imageView)
            if (optionIdCount.containsKey(item.id)) {
                holder.itemView.progressBar.visibility = View.VISIBLE
                holder.itemView.textView2.visibility = View.VISIBLE
                setCustomFontWithTextStyle(
                    holder.itemView.textView2,
                    "fonts/RingsideCompressed-Bold.otf"
                )
                val total = optionIdCount.values.reduce { acc, i -> acc + i }
                val percent = when (total > 0) {
                    true -> (optionIdCount[item.id!!]!!.toFloat() / total.toFloat()) * 100
                    else -> 0F
                }
                holder.itemView.progressBar.progress = percent.toInt()
                holder.itemView.textView2.text = "$percent %"
            } else {
                holder.itemView.progressBar.visibility = View.INVISIBLE
                holder.itemView.textView2.visibility = View.GONE
            }
            holder.itemView.textView.text = "${item.description}"
            setCustomFontWithTextStyle(holder.itemView.textView, "fonts/RingsideRegular-Book.otf")
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
                    pollListener?.onSelectOption(item.id!!)
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
                setCustomFontWithTextStyle(
                    holder.itemView.txt_percent,
                    "fonts/RingsideCompressed-Bold.otf"
                )
                holder.itemView.txt_percent.text = "$percent %"
                holder.itemView.progressBar_text.progress = percent.toInt()
            } else {
                holder.itemView.txt_percent.visibility = View.INVISIBLE
                holder.itemView.progressBar_text.visibility = View.INVISIBLE
            }
            holder.itemView.text_poll_item.text = "${item.description}"
            setCustomFontWithTextStyle(
                holder.itemView.text_poll_item,
                "fonts/RingsideRegular-Book.otf"
            )
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
                    pollListener?.onSelectOption(item.id!!)
                    notifyDataSetChanged()
                }
            else
                holder.itemView.lay_poll_text_option.setOnClickListener(null)
        }

    }

    override fun getItemCount(): Int = list.size
}
