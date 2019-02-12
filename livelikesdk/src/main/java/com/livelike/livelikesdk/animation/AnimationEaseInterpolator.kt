package com.livelike.livelikesdk.animation

import android.animation.ValueAnimator

interface AnimationEaseInterpolator {
    // We will add more as we encounter them
    enum class Ease {
        EaseInElastic,
        EaseOutElastic,
        EaseInBounce
    }

    fun createAnimationEffectWith(ease: Ease, forDuration: Float, animator: ValueAnimator) : ValueAnimator
}