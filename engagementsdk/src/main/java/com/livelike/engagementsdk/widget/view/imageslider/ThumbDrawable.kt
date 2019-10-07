package com.livelike.engagementsdk.widget.view.imageslider

import android.graphics.Canvas
import android.graphics.Rect

class ThumbDrawable(private val drawableList: List<ScaleDrawable>, val initialMagnitude: Float = .5f) : GenericDrawableCallback() {

    private lateinit var drawable: ScaleDrawable

    var progress = initialMagnitude
        set(value) {
            field = value
            if (drawableList.isNotEmpty()) {
                drawable = getDrawable(progress, drawableList.size)
                drawable.scale = getScale(progress)
            }
        }

    private fun getScale(progress: Float): Float {
        return if (drawableList.size > 2) {
            if (progress < .5) {
                1.5f - progress
            } else {
                (progress * 2) - .5f
            }
        } else {
            1 + progress / 2
        }
    }

    private fun getDrawable(progress: Float, size: Int): ScaleDrawable {
        return try {
            drawableList[(progress * size).toInt()]
        } catch (ex: IndexOutOfBoundsException) {
            drawableList[size - 1]
        }
    }

    init {
        progress = initialMagnitude
    }

    override fun draw(canvas: Canvas) {
        drawable.draw(canvas)
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        drawable.bounds = bounds
    }

    override fun getIntrinsicHeight(): Int = drawable.intrinsicHeight

    override fun getIntrinsicWidth(): Int = drawable.intrinsicWidth
}
