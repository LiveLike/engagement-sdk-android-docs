package com.mml.mmlengagementsdk.widgets

import android.content.Context
import android.graphics.Bitmap
import android.support.constraint.ConstraintLayout
import android.util.TypedValue
import android.view.View
import com.bumptech.glide.Glide
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.widget.widgetModel.ImageSliderWidgetModel
import com.mml.mmlengagementsdk.widgets.utils.getFormattedTime
import com.mml.mmlengagementsdk.widgets.utils.imageslider.ScaleDrawable
import com.mml.mmlengagementsdk.widgets.utils.imageslider.ThumbDrawable
import com.mml.mmlengagementsdk.widgets.utils.parseDuration
import com.mml.mmlengagementsdk.widgets.utils.setCustomFontWithTextStyle
import kotlinx.android.synthetic.main.mml_image_slider.view.image_slider
import kotlinx.android.synthetic.main.mml_image_slider.view.slider_title
import kotlinx.android.synthetic.main.mml_image_slider.view.time_bar
import kotlinx.android.synthetic.main.mml_image_slider.view.txt_time
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MMLImageSliderWidget(context: Context) : ConstraintLayout(context) {
    lateinit var imageSliderWidgetModel: ImageSliderWidgetModel
    var isTimeLine = false
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    init {
        inflate(context, R.layout.mml_image_slider, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        imageSliderWidgetModel.widgetData.let { liveLikeWidget ->
            slider_title.text = liveLikeWidget.question
            setCustomFontWithTextStyle(slider_title, "fonts/RingsideExtraWide-Black.otf")
            liveLikeWidget.createdAt?.let {
                setCustomFontWithTextStyle(txt_time, "fonts/RingsideRegular-Book.otf")
                txt_time.text = getFormattedTime(it)
            }
            val size = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                36f,
                resources.displayMetrics
            ).toInt()
            uiScope.launch {
                val list = mutableListOf<Deferred<Bitmap>>()
                withContext(Dispatchers.IO) {
                    liveLikeWidget.options?.forEach {
                        list.add(
                            async {
                                Glide.with(context)
                                    .asBitmap()
                                    .load(it?.imageUrl)
                                    .centerCrop().submit(size, size).get()
                            }
                        )
                    }
                    val drawableList = list.map { t ->
                        ScaleDrawable(
                            t.await()
                        )
                    }
                    withContext(Dispatchers.Main) {
                        val drawable =
                            ThumbDrawable(
                                drawableList,
                                .5f
                            )
                        image_slider.thumbDrawable = drawable
                    }
                }
            }
            if (isTimeLine) {
                image_slider.averageProgress = liveLikeWidget.averageMagnitude
                time_bar.visibility = View.INVISIBLE
                image_slider.isUserSeekable = false
            } else {
                image_slider.isUserSeekable = true
                val timeMillis = liveLikeWidget.timeout?.parseDuration() ?: 5000
                time_bar.startTimer(timeMillis)
                uiScope.async {
                    delay(timeMillis)
                    imageSliderWidgetModel.lockInVote(image_slider.progress.toDouble())
                    delay(5000)
                    imageSliderWidgetModel.finish()
                }
                imageSliderWidgetModel.voteResults.subscribe(this) {
                    it?.let {
                        image_slider.averageProgress = it.averageMagnitude
                    }
                }
            }
        }


    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        imageSliderWidgetModel.voteResults.unsubscribe(this)
        imageSliderWidgetModel.finish()
    }

}