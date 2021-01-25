package com.livelike.livelikedemo.mml.widgets.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.livelike.livelikedemo.R
import com.livelike.livelikedemo.mml.widgets.model.LiveLikeWidgetOption
import kotlinx.android.synthetic.main.image_option_list_item.view.option_iv
import kotlinx.android.synthetic.main.image_option_list_item.view.option_tv
import kotlinx.android.synthetic.main.image_option_list_item.view.result_bar
import kotlinx.android.synthetic.main.image_option_list_item.view.result_tv
import kotlin.math.max

class ImageOptionsWidgetAdapter(
    private val context: Context,
    var list: ArrayList<LiveLikeWidgetOption>,
    private val optionSelectListener: (LiveLikeWidgetOption) -> Unit
) :
    RecyclerView.Adapter<ImageOptionsWidgetAdapter.ImageOptionsListItemViewHolder>() {


    var isResultState: Boolean = false
    var isResultAvailable: Boolean = false

    /**
     * flag to tell whether to use red-green bar color to indicate right and wrong answers
     *  or otherwise just blue - grey to indicate selected and unselected answer
     */
    var indicateRightAnswer: Boolean = true

    var selectedOptionItem: LiveLikeWidgetOption? = null
    var currentlySelectedViewHolder: ImageOptionsListItemViewHolder? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageOptionsListItemViewHolder {
        return ImageOptionsListItemViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.image_option_list_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(
        holder: ImageOptionsListItemViewHolder,
        position: Int
    ) {
        val liveLikeWidgetOption = list[position]
        holder.view.option_tv.text = liveLikeWidgetOption.description
        Glide.with(context).load(liveLikeWidgetOption.imageUrl).into(holder.view.option_iv)

        if (isResultState && isResultAvailable) {
            holder.view.result_bar.visibility = View.VISIBLE
            holder.view.result_tv.visibility = View.VISIBLE
            holder.view.setOnClickListener(null)
            holder.view.result_tv.text = "${liveLikeWidgetOption.percentage ?: 0}%"
            holder.view.result_bar.pivotX = 0f
            holder.view.result_bar.scaleX =
                max(((liveLikeWidgetOption.percentage ?: 0) / 100f), 0.1f)

            if (!indicateRightAnswer) {
                if (selectedOptionItem?.id == liveLikeWidgetOption.id) {
                    holder.view.result_bar.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.selected_result_bar_color
                        )
                    )
                } else {
                    holder.view.result_bar.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.default_result_bar_color
                        )
                    )
                }
            } else {
                if (selectedOptionItem?.id == liveLikeWidgetOption.id && !liveLikeWidgetOption.isCorrect) {
                    holder.view.result_bar.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.incorrect_result_bar_color
                        )
                    )
                } else if (liveLikeWidgetOption.isCorrect) {
                    holder.view.result_bar.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.correct_result_bar_color
                        )
                    )
                } else {
                    holder.view.result_bar.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.default_result_bar_color
                        )
                    )
                }
            }
            if (selectedOptionItem?.id == liveLikeWidgetOption.id) {
                holder.selectOption()
            } else {
                holder.unSelectOption()
            }
            holder.view.setOnClickListener(null)
        } else {
            holder.view.result_bar.visibility = View.GONE
            holder.view.result_tv.visibility = View.GONE
            holder.view.setOnClickListener {
                currentlySelectedViewHolder?.unSelectOption()
                currentlySelectedViewHolder = holder
                holder.selectOption()
                selectedOptionItem = liveLikeWidgetOption
                optionSelectListener.invoke(liveLikeWidgetOption)
            }
        }

    }


    inner class ImageOptionsListItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        fun selectOption() {
            view.option_tv.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            view.result_tv.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            view.setBackgroundResource(R.drawable.image_option_background_selected_drawable)
        }

        fun unSelectOption() {
            view.option_tv.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            view.result_tv.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            view.setBackgroundResource(R.drawable.image_option_background_stroke_drawable)
        }
    }

}