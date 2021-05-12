package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.ImageSliderTheme
import com.livelike.engagementsdk.widget.model.ImageSliderEntity
import com.livelike.engagementsdk.widget.view.components.imageslider.ImageSlider
import com.livelike.engagementsdk.widget.view.components.imageslider.ScaleDrawable
import com.livelike.engagementsdk.widget.view.components.imageslider.ThumbDrawable
import com.livelike.engagementsdk.widget.viewModel.EmojiSliderWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetState
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView
import kotlinx.android.synthetic.main.widget_emoji_slider.view.image_slider
import kotlinx.android.synthetic.main.widget_emoji_slider.view.image_slider_widget_box
import kotlinx.android.synthetic.main.widget_emoji_slider.view.txtTitleBackground
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode

internal class EmojiSliderWidgetView(context: Context, attr: AttributeSet? = null) :
    GenericSpecifiedWidgetView<ImageSliderEntity, EmojiSliderWidgetViewModel>(context, attr) {
    override fun lockInteraction() {
        image_slider.isUserSeekable = false
    }

    override fun unLockInteraction() {
        image_slider.isUserSeekable = true
        viewModel?.markAsInteractive()
    }

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
        onWidgetInteractionCompleted()
    }

    override fun showResults() {
        val didUserVote = viewModel.currentVote.latest().isNullOrEmpty().not()
        val result = when (didUserVote) {
            true -> viewModel.results.latest()
            else -> viewModel.data.latest()
        }
        image_slider.averageProgress = result?.averageMagnitude ?: image_slider.progress

        //Commenting this code as we have to update image and red pointer both iin resul stage
//        if (!didUserVote) {
        result?.averageMagnitude?.let {
            image_slider.progress = it
        }
//        }
        logDebug { "EmojiSlider Widget showing result value:${image_slider.averageProgress}" }
    }

    private fun updateTitle(it: ImageSliderTheme?) {
        it?.let { sliderTheme ->
            applyThemeOnTitleView(sliderTheme)
            AndroidResource.createDrawable(sliderTheme.body)?.let {
                image_slider_widget_box.background = it
            }
            if (sliderTheme.header?.background != null) {
                txtTitleBackground.background = AndroidResource.createDrawable(sliderTheme.header)
            }
            sliderTheme.header?.padding?.let {
                AndroidResource.setPaddingForView(titleView, sliderTheme.header.padding)
            }
        }
    }

    override fun dataModelObserver(entity: ImageSliderEntity?) {
        entity?.let { resource ->
            resource.getMergedOptions() ?: return
            if (!isViewInflated) {
                inflate(context, R.layout.widget_emoji_slider, this)
                updateTitle(widgetsTheme?.imageSlider)
                titleTextView.gravity = Gravity.START
                titleView.title = resource.question
                if (image_slider.progress == ImageSlider.INITIAL_POSITION)
                    entity.initialMagnitude?.let {
                        image_slider.progress = it
                    }
                viewModel.currentVote.currentData?.let {
                    image_slider.progress = it.toFloat()
                }
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
                                    try {
                                        Glide.with(context.applicationContext)
                                            .asBitmap()
                                            .load(it.image_url)
                                            .centerCrop()
                                            .submit(size, size)
                                            .get()
                                    } catch (e: Exception) {
                                        BitmapFactory.decodeResource(
                                            context.resources,
                                            R.drawable.default_avatar
                                        )
                                    }
                                }
                            )
                        }
                        val drawableList = list.mapNotNull { t -> ScaleDrawable(t.await()) }
                        withContext(Dispatchers.Main) {
                            val drawable = ThumbDrawable(drawableList, .5f)
                            image_slider.thumbDrawable = drawable
                        }
                    }
                }

                image_slider.positionListener = { magnitude ->
                    viewModel.currentVote.onNext(
                        "${
                            magnitude.toBigDecimal().setScale(
                                2,
                                RoundingMode.UP
                            ).toFloat()
                        }"
                    )
                }
            }
        }
        logDebug { "showing EmojiSliderWidget" }
        super.dataModelObserver(entity)
    }
}
