package com.livelike.engagementsdk.widget.view.components.imageslider

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Animatable
import android.os.SystemClock
import android.view.animation.DecelerateInterpolator
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable

internal class ResultDrawable(
    val context: Context,
    private val centerColor: Int,
    private val sideColor: Int
) : GenericDrawableCallback(), Animatable, Runnable {

    private var isDrawResultGradient: Boolean = false
    private val FRAME_DELAY = (1000 / 60).toLong() // 60 fps
    private var mRunning = false
    private var mStartTime: Long = 0
    private val mDuration = 1000 // in ms

    val mLottieDrawable: LottieDrawable = LottieDrawable()

    val mInterpolator = DecelerateInterpolator()

    var mAverageProgress: Float? = null

    private val resultGradient = Paint(1)
    internal var totalHeight: Int = 0
    internal var trackHeight: Float = 0f

    init {
        mLottieDrawable.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                isDrawResultGradient = true
                start()
            }
        })
        LottieCompositionFactory.fromAsset(context, "image_slider_result.json")
            .addListener { composition ->
                mLottieDrawable.composition = composition
                mLottieDrawable.repeatCount = 0
            }
        resultGradient.color = sideColor
    }

    fun startLottieAnimation(callback: Callback) {
        mLottieDrawable.callback = callback
        mLottieDrawable.start()
    }

    override fun draw(canvas: Canvas) {
        if (isDrawResultGradient) {
            canvas.save()
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            val barRect = RectF()
            barRect.set(
                0f,
                bounds.height() / 2f - trackHeight / 2,
                bounds.width().toFloat(),
                bounds.height() / 2f + trackHeight / 2
            )
            if (isRunning) {
                val elapsed = (SystemClock.uptimeMillis() - mStartTime).toFloat()
                val rawProgress = elapsed / mDuration
                val progress = mInterpolator.getInterpolation(rawProgress)

                alpha = (progress * 255).toInt()
                canvas.drawRoundRect(barRect, trackHeight / 2, trackHeight / 2, resultGradient)
            } else {
                alpha = 255
                canvas.drawRoundRect(barRect, trackHeight / 2, trackHeight / 2, resultGradient)
            }
            canvas.restore()
        }
    }

    private fun updateShader(rect: Rect) {
        resultGradient.shader = LinearGradient(
            0.0f,
            rect.bottom.toFloat(),
            0.0f,
            rect.top.toFloat(),
            sideColor,
            centerColor,
            Shader.TileMode.CLAMP
        )
    }

    override fun setAlpha(alpha: Int) {
        this.resultGradient.alpha = alpha
    }

    override fun onBoundsChange(bounds: Rect?) {
        bounds?.let { updateShader(bounds) }
    }

    override fun start() {
        if (isRunning) {
            stop()
        }
        mRunning = true
        mStartTime = SystemClock.uptimeMillis()
        invalidateSelf()
        scheduleSelf(this, mStartTime + FRAME_DELAY)
    }

    override fun stop() {
        unscheduleSelf(this)
        mRunning = false
    }

    override fun run() {
        invalidateSelf()
        val uptimeMillis = SystemClock.uptimeMillis()
        if (uptimeMillis + FRAME_DELAY < mStartTime + mDuration) {
            scheduleSelf(this, uptimeMillis + FRAME_DELAY)
        } else {
            mRunning = false
            invalidateSelf()
        }
    }

    override fun isRunning(): Boolean {
        return mRunning
    }
}
