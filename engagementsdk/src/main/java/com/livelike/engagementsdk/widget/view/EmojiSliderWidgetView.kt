package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.widget.model.ImageSliderEntity
import com.livelike.engagementsdk.widget.view.components.imageslider.ImageSlider
import com.livelike.engagementsdk.widget.view.components.imageslider.ScaleDrawable
import com.livelike.engagementsdk.widget.view.components.imageslider.ThumbDrawable
import com.livelike.engagementsdk.widget.viewModel.EmojiSliderWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetState
import java.math.RoundingMode
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView
import kotlinx.android.synthetic.main.widget_emoji_slider.view.image_slider
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class EmojiSliderWidgetView(context: Context, attr: AttributeSet? = null) :
    GenericSpecifiedWidgetView<ImageSliderEntity, EmojiSliderWidgetViewModel>(context, attr) {

    override fun subscribeCalls() {
        super.subscribeCalls()
    }

    override fun unsubscribeCalls() {
        super.unsubscribeCalls()
    }

    override fun stateObserver(widgetState: WidgetState) {
        super.stateObserver(widgetState)
    }

    override fun confirmInteraction() {
        image_slider.isUserSeekable = false
    }

    override fun showResults() {
        val result = viewModel.results.latest()
        val averageMagnitude = result?.averageMagnitude ?: image_slider.progress
        image_slider.averageProgress = averageMagnitude
    }

    override fun dataModelObserver(entity: ImageSliderEntity?) {
        entity?.let { resource ->
            resource.getMergedOptions() ?: return
            if (!isViewInflated) {
                inflate(context, R.layout.widget_emoji_slider, this)
                titleTextView.background =
                    ColorDrawable(ContextCompat.getColor(context, R.color.livelike_transparent))
                titleView.title = resource.question
                if (image_slider.progress == ImageSlider.INITIAL_POSITION)
                    image_slider.progress = entity.initialMagnitude
                val size = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    36f,
                    resources.displayMetrics
                ).toInt()
                viewModel.uiScope.launch {
                    val list = mutableListOf<Deferred<Bitmap>>()
                    withContext(Dispatchers.IO) {
                        resource.options?.forEach {
                            list.add(
                                async {
                                    Glide.with(context)
                                        .asBitmap()
                                        .load(it.image_url).centerCrop().submit(size, size).get()
                                }
                            )
                        }
                        val drawableList = list.map { t -> ScaleDrawable(t.await()) }
                        withContext(Dispatchers.Main) {
                            val drawable = ThumbDrawable(drawableList, .5f)
                            image_slider.thumbDrawable = drawable
                        }
                    }
                }

                image_slider.positionListener = { magnitude ->
                    viewModel.currentVote.onNext(
                        "${magnitude.toBigDecimal().setScale(
                            2,
                            RoundingMode.UP
                        ).toFloat()}"
                    )
                }
            }
        }
        super.dataModelObserver(entity)
    }
}
