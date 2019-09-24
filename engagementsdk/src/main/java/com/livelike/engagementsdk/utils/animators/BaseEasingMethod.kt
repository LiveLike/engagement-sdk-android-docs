package com.livelike.engagementsdk.utils.animators

import android.animation.TypeEvaluator

/**
 * Refer : https://github.com/daimajia/AnimationEasingFunctions,
 */
abstract class BaseEasingMethod(private var mDuration: Float) : TypeEvaluator<Number> {

    private val mListeners = ArrayList<EasingListener>()

    interface EasingListener {
        fun on(time: Float, value: Float, start: Float, end: Float, duration: Float)
    }

    fun addEasingListener(l: EasingListener) {
        mListeners.add(l)
    }

    fun addEasingListeners(vararg ls: EasingListener) {
        for (l in ls) {
            mListeners.add(l)
        }
    }

    fun removeEasingListener(l: EasingListener) {
        mListeners.remove(l)
    }

    fun clearEasingListeners() {
        mListeners.clear()
    }

    fun setDuration(duration: Float) {
        mDuration = duration
    }

    override fun evaluate(fraction: Float, startValue: Number, endValue: Number): Float? {
        val t = mDuration * fraction
        val b = startValue.toFloat()
        val c = endValue.toFloat() - startValue.toFloat()
        val d = mDuration
        val result = calculate(t, b, c, d)!!
        for (l in mListeners) {
            l.on(t, result, b, c, d)
        }
        return result
    }

    abstract fun calculate(t: Float, b: Float, c: Float, d: Float): Float?
}
