package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.livelike.engagementsdk.widget.widgetModel.ImageSliderWidgetModel
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_image_slider.view.gauge_seek_bar
import kotlinx.android.synthetic.main.custom_image_slider.view.imageButton3
import kotlinx.android.synthetic.main.custom_image_slider.view.txt_result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


class CustomImageSlider : ConstraintLayout {
    
    lateinit var imageSliderWidgetModel: ImageSliderWidgetModel

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        inflate(context, R.layout.custom_image_slider, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        imageSliderWidgetModel.widgetData.let { widget ->
            widget.options?.get(0)?.imageUrl?.let {
                gauge_seek_bar.setThumbImage(it)
            }
            val slide = debounce<Float>(500, coroutineContext) {
                imageSliderWidgetModel.lockInVote(it.toDouble())
            }
            gauge_seek_bar.progressChangedCallback = {
                slide(it)
            }
        }

        imageSliderWidgetModel.voteResults.subscribe(this) {
            it?.let {
                txt_result.text = "Result: ${it.averageMagnitude}"
            }
        }
        imageButton3.setOnClickListener {
            imageSliderWidgetModel.finish()
        }
    }

    private fun <T> debounce(
        delayMs: Long = 500L,
        coroutineContext: CoroutineContext,
        f: (T) -> Unit
    ): (T) -> Unit {
        var debounceJob: Job? = null
        return { param: T ->
            if (debounceJob?.isCompleted != false) {
                debounceJob = CoroutineScope(coroutineContext).launch {
                    delay(delayMs)
                    f(param)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        imageSliderWidgetModel.voteResults.unsubscribe(this)
    }


}