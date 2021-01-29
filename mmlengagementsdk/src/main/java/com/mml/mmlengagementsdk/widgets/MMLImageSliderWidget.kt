package com.mml.mmlengagementsdk.widgets

import android.content.Context
import android.graphics.Bitmap
import android.support.constraint.ConstraintLayout
import android.util.TypedValue
import android.view.View
import com.bumptech.glide.Glide
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.widget.widgetModel.ImageSliderWidgetModel
import com.mml.mmlengagementsdk.widgets.timeline.TimelineWidgetResource
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.max

class MMLImageSliderWidget(context: Context) : ConstraintLayout(context) {
    lateinit var imageSliderWidgetModel: ImageSliderWidgetModel
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    var timelineWidgetResource: TimelineWidgetResource? = null

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
            if (timelineWidgetResource?.isActive == false) {
                image_slider.averageProgress =
                    timelineWidgetResource?.liveLikeWidgetResult?.averageMagnitude ?: liveLikeWidget.averageMagnitude
                time_bar.visibility = View.INVISIBLE
                image_slider.isUserSeekable = false
            } else {
                image_slider.isUserSeekable = true
                if (timelineWidgetResource?.startTime == null) {
                    timelineWidgetResource?.startTime = Calendar.getInstance().timeInMillis
                }
                val timeMillis = liveLikeWidget.timeout?.parseDuration() ?: 5000
                val timeDiff =
                    Calendar.getInstance().timeInMillis - (timelineWidgetResource?.startTime ?: 0L)
                val remainingTimeMillis = max(0, timeMillis - timeDiff)
                time_bar.visibility = View.VISIBLE
                time_bar.startTimer(timeMillis, remainingTimeMillis)

                uiScope.async {
                    delay(remainingTimeMillis)
                    imageSliderWidgetModel.lockInVote(image_slider.progress.toDouble())
                    imageSliderWidgetModel.voteResults.subscribe(this@MMLImageSliderWidget) {
                        it?.let {
                            if (image_slider.averageProgress != it.averageMagnitude) {
                                image_slider.averageProgress = it.averageMagnitude
                            }
                        }
                        timelineWidgetResource?.liveLikeWidgetResult = it
                    }
                    delay(2000)
                    timelineWidgetResource?.isActive = false
                    imageSliderWidgetModel.voteResults.unsubscribe(this@MMLImageSliderWidget)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (timelineWidgetResource?.isActive == true) {
            job.cancel()
            uiScope.cancel()
            imageSliderWidgetModel.voteResults.unsubscribe(this)
            imageSliderWidgetModel.finish()
        }
    }

}