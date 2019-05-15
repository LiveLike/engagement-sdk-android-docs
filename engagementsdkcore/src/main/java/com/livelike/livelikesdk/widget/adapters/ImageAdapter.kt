package com.livelike.livelikesdk.widget.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.util.WidgetResultDisplayUtil
import kotlinx.android.synthetic.main.prediction_image_row_element.view.button
import kotlinx.android.synthetic.main.prediction_image_row_element.view.determinateBar
import kotlinx.android.synthetic.main.prediction_image_row_element.view.image_button
import kotlinx.android.synthetic.main.prediction_image_row_element.view.item_text
import kotlinx.android.synthetic.main.prediction_image_row_element.view.percentageText

class ImageAdapter internal constructor(
    private val optionList: List<VoteOption>,
    private val optionSelectedCallback: (String?) -> Unit,
    private val parentWidth: Int,
    private val context: Context,
    private val resultDisplayUtil: WidgetResultDisplayUtil,
    private val adapterUpdateCompleteCallback: (imageLoadSuccess: Boolean) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    private var imageLoadedCount = 0
    val viewOptions = HashMap<String?, ViewOption>()
    var correctOption: String? = ""

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = optionList[position]
        holder.optionText.text = option.description
        option.isCorrect.let { if (it) correctOption = option.id }

        holder.loadImage(context, option.imageUrl) {
            if (!it) adapterUpdateCompleteCallback.invoke(false)
            if (++imageLoadedCount >= optionList.size)
                adapterUpdateCompleteCallback.invoke(true)
        }

        holder.button.setOnClickListener {
            optionSelectedCallback.invoke(option.id)
        }

        viewOptions[option.id] = ViewOption(holder.button, holder.progressBar, holder.percentageText)
        resultDisplayUtil.setImageViewMargin(option, optionList, holder.itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(
            R.layout.prediction_image_row_element,
            parent,
            false
        )

        resultDisplayUtil.setImageItemWidth(optionList, view, parentWidth)
        return ViewHolder(
            view
        )
    }

    override fun getItemCount(): Int {
        return optionList.size
    }

    class ViewOption(
        val button: View,
        val progressBar: ProgressBar,
        val percentageTextView: TextView
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: View = view.button
        val optionButton: ImageView = view.image_button
        val optionText: TextView = view.item_text
        val percentageText: TextView = view.percentageText
        val progressBar: ProgressBar = view.determinateBar

        fun loadImage(context: Context, imageUrl: String, imageLoadedCallback: (success: Boolean) -> Unit) {
            Glide.with(context)
                .load(imageUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        imageLoadedCallback(false)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        imageLoadedCallback(true)
                        return false
                    }
                })
                .apply(
                    RequestOptions().override(AndroidResource.dpToPx(74), AndroidResource.dpToPx(74)).transform(
                        MultiTransformation(FitCenter(), RoundedCorners(12))
                    )
                )
                .into(optionButton)
        }
    }
}
