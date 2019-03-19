package com.livelike.livelikesdk.animation.easing

import android.animation.ValueAnimator

interface AnimationEaseInterpolator {
    // We will add more as we encounter them
    enum class Ease {
        EaseInElastic,
        EaseOutElastic,
        EaseInBounce,
        EaseOutQuad,
        EaseOutCubic
    }

    fun createAnimationEffectWith(ease: Ease, forDuration: Float, animator: ValueAnimator) : ValueAnimator
}